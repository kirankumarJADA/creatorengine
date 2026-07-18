package com.creatorengine.aifaq.service;

import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.creatorengine.aifaq.entity.QaPair;
import com.creatorengine.aifaq.repository.AiFaqConfigRepository;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.automation.ratelimit.RateLimitService;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.MetaMessagingService;
import com.creatorengine.instagram.service.MetaMessagingService.AccessTokenContext;
import com.creatorengine.instagram.service.MetaMessagingService.ByUserId;
import com.creatorengine.instagram.service.MetaMessagingService.Recipient;
import com.creatorengine.instagram.service.MetaMessagingService.SendResult;
import com.creatorengine.plan.entity.Plan;
import com.creatorengine.plan.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * AI FAQ (#14): when a DM doesn't match any keyword automation, and the
 * creator has this enabled and is on a Pro/Agency plan, Gemini answers
 * using the creator's curated Q&amp;A pairs and free-text knowledge base.
 */
@Service
public class AiFaqService {

    private static final Logger log = LoggerFactory.getLogger(AiFaqService.class);

    private static final int MAX_QA_PAIRS = 50;
    private static final int MAX_KNOWLEDGE_BASE_CHARS = 8000;
    private static final int MAX_REPLY_CHARS = 900;

    private final AiFaqConfigRepository configRepository;
    private final NvidiaGptClient aiClient;
    private final PlanService planService;
    private final InstagramAccountService instagramAccountService;
    private final MetaMessagingService metaMessaging;
    private final RateLimitService rateLimitService;
    private final ContactService contactService;
    private final JobQueue queue;

    public AiFaqService(
            AiFaqConfigRepository configRepository,
            NvidiaGptClient aiClient,
            PlanService planService,
            InstagramAccountService instagramAccountService,
            MetaMessagingService metaMessaging,
            RateLimitService rateLimitService,
            ContactService contactService,
            JobQueue queue
    ) {
        this.configRepository = configRepository;
        this.aiClient = aiClient;
        this.planService = planService;
        this.instagramAccountService = instagramAccountService;
        this.metaMessaging = metaMessaging;
        this.rateLimitService = rateLimitService;
        this.contactService = contactService;
        this.queue = queue;
    }

    // ─── Settings CRUD (used by AiFaqController) ──────────────────

    public AiFaqConfig fetch(String uid, String igAccountId) {
        return configRepository.find(uid, igAccountId);
    }

    public AiFaqConfig save(String uid, String igAccountId, AiFaqConfig incoming) {
        AiFaqConfig config = new AiFaqConfig();
        config.setEnabled(incoming != null && incoming.getEnabled());

        String kb = incoming != null ? incoming.getKnowledgeBase() : null;
        if (kb != null && kb.length() > MAX_KNOWLEDGE_BASE_CHARS) {
            kb = kb.substring(0, MAX_KNOWLEDGE_BASE_CHARS);
        }
        config.setKnowledgeBase(kb != null ? kb : "");

        List<QaPair> pairs = incoming != null && incoming.getQaPairs() != null
                ? incoming.getQaPairs().stream()
                        .filter(p -> p != null && p.getQuestion() != null && !p.getQuestion().isBlank())
                        .limit(MAX_QA_PAIRS)
                        .toList()
                : List.of();
        config.setQaPairs(pairs);

        return configRepository.save(uid, igAccountId, config);
    }

    public boolean isPlanEligible(String uid) {
        return planService.getPlan(uid).isProOrHigher();
    }

    /**
     * "Test AI" — lets the creator try a question against their DRAFT
     * knowledge base / Q&A (not yet saved) before enabling the feature.
     * Never sends a real DM. Lightly rate-limited per uid to control cost.
     */
    public String testAnswer(String uid, AiFaqConfig draftConfig, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Enter a test question first.");
        }
        if (draftConfig == null || !draftConfig.hasContent()) {
            throw new IllegalArgumentException("Add a knowledge base or at least one Q&A pair to test.");
        }
        if (!rateLimitService.tryAcquire("aifaq-test:" + uid)) {
            throw new IllegalStateException("Too many test requests - please wait a moment and try again.");
        }

        String systemInstruction = buildSystemInstruction(draftConfig);
        String answer = aiClient.generateAnswer(systemInstruction, message.trim());

        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("AI returned an empty answer.");
        }
        if (answer.length() > MAX_REPLY_CHARS) {
            answer = answer.substring(0, MAX_REPLY_CHARS);
        }
        return answer;
    }

    // ─── Fallback answering (called from AutomationEngine's queue) ─

    /** Sentinel automationId routed here from AutomationEngine.processJob. */
    public static final String JOB_MARKER = "__AI_FAQ__";

    public void handleJob(AutomationJob job) {
        String uid = job.uid();
        WebhookEventDto event = job.event();
        String igAccountId = job.igAccountId();

        Plan plan = planService.getPlan(uid);
        if (!plan.isProOrHigher()) {
            log.info("AI FAQ skipped uid={} - plan {} does not include AI features.", uid, plan);
            return;
        }

        AiFaqConfig config = configRepository.find(uid, igAccountId);
        if (!config.getEnabled()) {
            log.info("AI FAQ skipped uid={} - feature disabled.", uid);
            return;
        }
        if (!config.hasContent()) {
            log.info("AI FAQ skipped uid={} - no knowledge base or Q&A pairs configured.", uid);
            return;
        }

        String userMessage = event.message();
        if (userMessage == null || userMessage.isBlank()) {
            log.info("AI FAQ skipped uid={} - empty message text.", uid);
            return;
        }

        InstagramAccount account = igAccountId != null && !igAccountId.isBlank()
                ? instagramAccountService.findByIgId(uid, igAccountId).orElse(null)
                : instagramAccountService.find(uid).orElse(null);

        if (account == null) {
            log.warn("AI FAQ: no connected Instagram account for uid={} igAccountId={}", uid, igAccountId);
            return;
        }

        String rateKey = account.getInstagramUserId();
        if (rateKey != null && !rateLimitService.tryAcquire(rateKey)) {
            Duration backoff = rateLimitService.suggestedBackoff(rateKey);
            if (backoff.isZero() || backoff.isNegative()) {
                backoff = Duration.ofSeconds(5);
            }
            queue.enqueueDelayed(job, backoff);
            log.info("AI FAQ rate-limited for ig={}, deferring {}s", rateKey, backoff.toSeconds());
            return;
        }

        String systemInstruction = buildSystemInstruction(config);

        String answer;
        try {
            answer = aiClient.generateAnswer(systemInstruction, userMessage);
        } catch (Exception ex) {
            log.warn("AI FAQ: AI call failed for uid={}: {}", uid, ex.getMessage());
            return;
        }

        if (answer == null || answer.isBlank()) {
            log.warn("AI FAQ: AI returned an empty answer for uid={}", uid);
            return;
        }

        if (answer.length() > MAX_REPLY_CHARS) {
            answer = answer.substring(0, MAX_REPLY_CHARS);
        }

        Recipient recipient = new ByUserId(event.instagramUserId());
        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(account.getInstagramUserId())
                .pageAccessToken(account.getAccessToken())
                .build();

        SendResult result = metaMessaging.sendText(recipient, answer, tokenCtx);

        if (result.success()) {
            contactService.recordFromEvent(uid, event, answer);
            log.info("AI FAQ answered uid={} ig={} messageId={}", uid, rateKey, result.messageId());
        } else {
            log.warn("AI FAQ send failed uid={} ig={}: {}", uid, rateKey, result.error());
        }
    }

    private String buildSystemInstruction(AiFaqConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are the Instagram DM assistant for a creator's business account. \
                Answer the customer's question using ONLY the information provided below. \
                Be warm, concise, and conversational, like a real person replying to a DM \
                - not a formal email and not customer-service boilerplate. \
                Keep your reply under 500 characters. \
                If the question cannot be answered from the information below, \
                politely say you'll check and get back to them - never invent facts, \
                prices, availability, or policies that aren't stated below.
                """);

        List<QaPair> pairs = config.getQaPairs();
        if (pairs != null && !pairs.isEmpty()) {
            sb.append("\n--- CURATED Q&A (use these first if the question is close to one of these) ---\n");
            for (QaPair p : pairs) {
                if (p.getQuestion() == null || p.getQuestion().isBlank()) continue;
                sb.append("Q: ").append(p.getQuestion().trim()).append('\n');
                sb.append("A: ").append(p.getAnswer() != null ? p.getAnswer().trim() : "").append("\n\n");
            }
        }

        String kb = config.getKnowledgeBase();
        if (kb != null && !kb.isBlank()) {
            sb.append("\n--- GENERAL BUSINESS INFO ---\n").append(kb.trim()).append('\n');
        }

        return sb.toString();
    }
}
