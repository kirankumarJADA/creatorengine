package com.creatorengine.autopilot.service;

import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.creatorengine.aifaq.repository.AiFaqConfigRepository;
import com.creatorengine.aifaq.service.NvidiaGptClient;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.auth.service.ResendEmailService;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.automation.ratelimit.RateLimitService;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.autopilot.entity.AllowedActions;
import com.creatorengine.autopilot.entity.AutopilotConfig;
import com.creatorengine.autopilot.entity.AutopilotConversation;
import com.creatorengine.autopilot.entity.AutopilotMessage;
import com.creatorengine.autopilot.entity.MessageTemplate;
import com.creatorengine.autopilot.repository.AutopilotConfigRepository;
import com.creatorengine.autopilot.repository.AutopilotConversationRepository;
import com.creatorengine.contacts.repository.ContactRepository;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Autopilot (#15): a full conversational AI sales/support agent, distinct
 * from AI FAQ (#14). Where AI FAQ answers one-off questions from a curated
 * Q&amp;A + knowledge base, Autopilot holds multi-turn conversations with
 * memory, collects lead data, qualifies leads, recommends products, and can
 * escalate to a human. Pro/Agency plan only, configured per Instagram
 * account. Takes priority over AI FAQ when enabled so the two never both
 * answer the same message (see AutomationEngine.dispatch()).
 */
@Service
public class AutopilotService {

    private static final Logger log = LoggerFactory.getLogger(AutopilotService.class);

    /** Sentinel automationId routed here from AutomationEngine.processJob. */
    public static final String JOB_MARKER = "__AI_AUTOPILOT__";

    private static final int MAX_HISTORY_MESSAGES = 20; // ~10 turns of context
    private static final int MAX_REPLY_CHARS = 900;
    private static final int MAX_TOKENS = 400;
    private static final int MAX_TEMPLATES = 20;
    private static final int MAX_ALLOWED_AUTOMATIONS = 20;

    private final AutopilotConfigRepository configRepository;
    private final AutopilotConversationRepository conversationRepository;
    private final AiFaqConfigRepository aiFaqConfigRepository; // shared knowledge base — no duplication
    private final NvidiaGptClient aiClient;
    private final PlanService planService;
    private final InstagramAccountService instagramAccountService;
    private final MetaMessagingService metaMessaging;
    private final RateLimitService rateLimitService;
    private final ContactService contactService;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ResendEmailService emailService;
    private final AutomationRepository automationRepository;
    private final JobQueue queue;
    private final ObjectMapper json;

    public AutopilotService(
            AutopilotConfigRepository configRepository,
            AutopilotConversationRepository conversationRepository,
            AiFaqConfigRepository aiFaqConfigRepository,
            NvidiaGptClient aiClient,
            PlanService planService,
            InstagramAccountService instagramAccountService,
            MetaMessagingService metaMessaging,
            RateLimitService rateLimitService,
            ContactService contactService,
            ContactRepository contactRepository,
            UserRepository userRepository,
            ResendEmailService emailService,
            AutomationRepository automationRepository,
            JobQueue queue,
            ObjectMapper json
    ) {
        this.configRepository = configRepository;
        this.conversationRepository = conversationRepository;
        this.aiFaqConfigRepository = aiFaqConfigRepository;
        this.aiClient = aiClient;
        this.planService = planService;
        this.instagramAccountService = instagramAccountService;
        this.metaMessaging = metaMessaging;
        this.rateLimitService = rateLimitService;
        this.contactService = contactService;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.automationRepository = automationRepository;
        this.queue = queue;
        this.json = json;
    }

    // ─── Settings CRUD (used by AutopilotController) ──────────────

    public AutopilotConfig fetch(String uid, String igAccountId) {
        return configRepository.find(uid, igAccountId);
    }

    public AutopilotConfig save(String uid, String igAccountId, AutopilotConfig incoming) {
        AutopilotConfig config = new AutopilotConfig();
        config.setEnabled(incoming != null && incoming.getEnabled());
        config.setRole(incoming != null && incoming.getRole() != null ? incoming.getRole() : config.getRole());
        config.setSystemPrompt(trim(incoming != null ? incoming.getSystemPrompt() : null, 4000));
        config.setGoal(trim(incoming != null ? incoming.getGoal() : null, 500));
        config.setTone(incoming != null && incoming.getTone() != null && !incoming.getTone().isBlank()
                ? incoming.getTone().trim() : "friendly");
        config.setAllowedActions(incoming != null ? incoming.getAllowedActions() : new AllowedActions());

        int timeout = incoming != null ? incoming.getConversationTimeoutMinutes() : 30;
        config.setConversationTimeoutMinutes(Math.max(5, Math.min(timeout, 24 * 60)));

        config.setFallbackMessage(trim(incoming != null ? incoming.getFallbackMessage() : null, 500));

        List<MessageTemplate> templates = incoming != null && incoming.getMessageTemplates() != null
                ? incoming.getMessageTemplates().stream()
                        .filter(t -> t != null && t.getLabel() != null && !t.getLabel().isBlank()
                                && t.getMessage() != null && !t.getMessage().isBlank())
                        .limit(MAX_TEMPLATES)
                        .peek(t -> {
                            if (t.getId() == null || t.getId().isBlank()) {
                                t.setId(java.util.UUID.randomUUID().toString());
                            }
                            t.setLabel(trim(t.getLabel(), 80));
                            t.setDescription(trim(t.getDescription(), 200));
                            t.setMessage(trim(t.getMessage(), 900));
                        })
                        .toList()
                : List.of();
        config.setMessageTemplates(templates);

        // Only keep automation IDs that still exist and are enabled for this
        // account — the AI must never be able to invoke something the owner
        // has since deleted or disabled elsewhere.
        List<String> requestedAutomationIds = incoming != null && incoming.getAllowedAutomationIds() != null
                ? incoming.getAllowedAutomationIds()
                : List.of();
        List<String> validAutomationIds = requestedAutomationIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(MAX_ALLOWED_AUTOMATIONS)
                .filter(id -> automationRepository.findById(uid, igAccountId, id).isPresent())
                .toList();
        config.setAllowedAutomationIds(validAutomationIds);

        return configRepository.save(uid, igAccountId, config);
    }

    public boolean isPlanEligible(String uid) {
        return planService.getPlan(uid).isProOrHigher();
    }

    /** Used by AutomationEngine to decide whether Autopilot should intercept before AI FAQ. */
    public boolean isEligibleAndEnabled(String uid, String igAccountId) {
        if (!isPlanEligible(uid)) return false;
        AutopilotConfig config = configRepository.find(uid, igAccountId);
        return config.getEnabled() && config.hasContent();
    }

    public AutopilotStats stats(String uid, String igAccountId) {
        AutopilotConfig config = configRepository.find(uid, igAccountId);
        List<AutopilotConversation> conversations = conversationRepository.listForAccount(uid, igAccountId);

        long conversationCount = conversations.size();
        long escalations = conversations.stream().filter(AutopilotConversation::getEscalated).count();
        long qualified = conversations.stream().filter(AutopilotConversation::getQualified).count();

        long totalResponseMs = conversations.stream().mapToLong(AutopilotConversation::getTotalResponseTimeMs).sum();
        long totalResponses = conversations.stream().mapToLong(AutopilotConversation::getResponseCount).sum();
        long avgResponseMs = totalResponses > 0 ? totalResponseMs / totalResponses : 0;

        return new AutopilotStats(config.getEnabled(), conversationCount, avgResponseMs, conversationCount, escalations, qualified);
    }

    public record AutopilotStats(
            boolean enabled, long conversationCount, long avgResponseTimeMs,
            long contactsHandled, long escalations, long qualifiedLeads
    ) {}

    // ─── Conversation handling (called from AutomationEngine's queue) ─

    public void handleJob(AutomationJob job) {
        String uid = job.uid();
        WebhookEventDto event = job.event();
        String igAccountId = job.igAccountId();
        String instagramUserId = event.instagramUserId();

        Plan plan = planService.getPlan(uid);
        if (!plan.isProOrHigher()) {
            log.info("Autopilot skipped uid={} - plan {} does not include AI features.", uid, plan);
            return;
        }

        AutopilotConfig config = configRepository.find(uid, igAccountId);
        if (!config.getEnabled() || !config.hasContent()) {
            log.info("Autopilot skipped uid={} - disabled or not configured.", uid);
            return;
        }

        String userMessage = event.message();
        if (userMessage == null || userMessage.isBlank() || instagramUserId == null) {
            log.info("Autopilot skipped uid={} - empty message or missing sender.", uid);
            return;
        }

        InstagramAccount account = igAccountId != null && !igAccountId.isBlank()
                ? instagramAccountService.findByIgId(uid, igAccountId).orElse(null)
                : instagramAccountService.find(uid).orElse(null);
        if (account == null) {
            log.warn("Autopilot: no connected Instagram account for uid={} igAccountId={}", uid, igAccountId);
            return;
        }

        String rateKey = account.getInstagramUserId();
        if (rateKey != null && !rateLimitService.tryAcquire(rateKey)) {
            Duration backoff = rateLimitService.suggestedBackoff(rateKey);
            if (backoff.isZero() || backoff.isNegative()) backoff = Duration.ofSeconds(5);
            queue.enqueueDelayed(job, backoff);
            log.info("Autopilot rate-limited for ig={}, deferring {}s", rateKey, backoff.toSeconds());
            return;
        }

        AutopilotConversation conversation = loadOrResetConversation(uid, igAccountId, instagramUserId, config, event);

        if (conversation.getEscalated()) {
            log.info("Autopilot: conversation already escalated to human, staying silent. uid={} ig={}", uid, instagramUserId);
            return;
        }

        conversation.getMessages().add(new AutopilotMessage("user", userMessage, new Date()));
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversation.setLastMessageAt(new Date());
        trimHistory(conversation);

        AiFaqConfig knowledge = aiFaqConfigRepository.find(uid, igAccountId);
        AllowedActions actions = config.getAllowedActions();
        List<Automation> triggerableAutomations = actions.getTriggerAutomations()
                ? loadTriggerableAutomations(uid, igAccountId, config)
                : List.of();

        String systemInstruction = buildSystemInstruction(config, knowledge, actions, triggerableAutomations);
        List<Map<String, String>> chatMessages = buildChatMessages(systemInstruction, conversation.getMessages());

        long startedAt = System.currentTimeMillis();
        String rawResponse;
        try {
            rawResponse = aiClient.chatCompletion(chatMessages, MAX_TOKENS);
        } catch (Exception ex) {
            log.warn("Autopilot: AI call failed for uid={}: {}", uid, ex.getMessage());
            return;
        }
        long responseTimeMs = System.currentTimeMillis() - startedAt;

        AutopilotTurn turn = parseTurn(rawResponse);
        if (turn == null || turn.reply() == null || turn.reply().isBlank()) {
            log.warn("Autopilot: AI returned no usable reply for uid={}", uid);
            return;
        }

        boolean escalateNow = turn.escalate() && actions.getEscalateToHuman();

        // Resolve any action the model chose (send a canned template, or
        // trigger an existing automation) — ignored entirely if escalating,
        // since the fallback message takes over in that case.
        MessageTemplate chosenTemplate = null;
        Automation chosenAutomation = null;
        if (!escalateNow && turn.actionId() != null) {
            if ("send_template".equals(turn.actionType()) && actions.getSendTemplates()) {
                chosenTemplate = config.getMessageTemplates().stream()
                        .filter(t -> turn.actionId().equals(t.getId()))
                        .findFirst().orElse(null);
            } else if ("trigger_automation".equals(turn.actionType()) && actions.getTriggerAutomations()) {
                chosenAutomation = triggerableAutomations.stream()
                        .filter(a -> turn.actionId().equals(a.getId()))
                        .findFirst().orElse(null);
            }
        }

        String outgoingText;
        if (escalateNow) {
            outgoingText = config.getFallbackMessage() != null && !config.getFallbackMessage().isBlank()
                    ? config.getFallbackMessage() : turn.reply();
        } else if (chosenTemplate != null) {
            outgoingText = chosenTemplate.getMessage();
        } else {
            outgoingText = turn.reply();
        }

        if (outgoingText.length() > MAX_REPLY_CHARS) {
            outgoingText = outgoingText.substring(0, MAX_REPLY_CHARS);
        }

        Recipient recipient = new ByUserId(instagramUserId);
        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(account.getInstagramUserId())
                .pageAccessToken(account.getAccessToken())
                .build();

        SendResult result = metaMessaging.sendText(recipient, outgoingText, tokenCtx);
        if (!result.success()) {
            log.warn("Autopilot send failed uid={} ig={}: {}", uid, rateKey, result.error());
            return;
        }

        conversation.getMessages().add(new AutopilotMessage("assistant", outgoingText, new Date()));
        conversation.setResponseCount(conversation.getResponseCount() + 1);
        conversation.setTotalResponseTimeMs(conversation.getTotalResponseTimeMs() + responseTimeMs);
        if (escalateNow) conversation.setEscalated(true);
        if (turn.qualified()) conversation.setQualified(true);
        applyCollectedData(conversation, turn, actions);
        conversationRepository.save(uid, igAccountId, conversation);

        contactService.recordFromEvent(uid, event, outgoingText);
        syncContact(uid, igAccountId, conversation, turn, actions);

        if (actions.getNotifyOwner() && (escalateNow || turn.qualified())) {
            notifyOwner(uid, conversation, escalateNow);
        }

        // Trigger the chosen automation's own action chain (tags, delays,
        // further DMs, etc.) through the normal queue — never executed
        // inline, so it respects that automation's own enabled/cooldown
        // behavior exactly like a manually-fired one would.
        if (chosenAutomation != null) {
            AutomationJob triggeredJob = AutomationJob.fresh(uid, event, chosenAutomation.getId())
                    .withIgAccountId(igAccountId);
            queue.enqueue(triggeredJob);
            log.info("Autopilot triggered automation {} for uid={} ig={}",
                    chosenAutomation.getId(), uid, instagramUserId);
        }

        log.info("Autopilot answered uid={} ig={} escalated={} qualified={} action={} messageId={}",
                uid, rateKey, escalateNow, turn.qualified(), turn.actionType(), result.messageId());
    }

    private List<Automation> loadTriggerableAutomations(String uid, String igAccountId, AutopilotConfig config) {
        List<String> ids = config.getAllowedAutomationIds();
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(id -> automationRepository.findById(uid, igAccountId, id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(Automation::getEnabled) // re-check live — owner may have since disabled it
                .toList();
    }

    private AutopilotConversation loadOrResetConversation(
            String uid, String igAccountId, String instagramUserId, AutopilotConfig config, WebhookEventDto event
    ) {
        AutopilotConversation existing = conversationRepository.find(uid, igAccountId, instagramUserId);
        long timeoutMs = Math.max(5, config.getConversationTimeoutMinutes()) * 60_000L;

        boolean expired = existing != null && existing.getLastMessageAt() != null
                && (System.currentTimeMillis() - existing.getLastMessageAt().getTime()) > timeoutMs;

        if (existing == null || expired) {
            AutopilotConversation fresh = new AutopilotConversation();
            fresh.setUid(uid);
            fresh.setIgAccountId(igAccountId);
            fresh.setInstagramUserId(instagramUserId);
            fresh.setUsername(event.username());
            fresh.setStartedAt(new Date());
            // Preserve prior collected data across a timeout-reset conversation —
            // it's still the same contact, just a fresh chat session.
            if (existing != null) {
                fresh.setCollectedName(existing.getCollectedName());
                fresh.setCollectedEmail(existing.getCollectedEmail());
                fresh.setCollectedPhone(existing.getCollectedPhone());
                fresh.setCollectedPreferences(existing.getCollectedPreferences());
                fresh.setCollectedBudget(existing.getCollectedBudget());
            }
            return fresh;
        }

        existing.setUsername(event.username() != null ? event.username() : existing.getUsername());
        return existing;
    }

    private void trimHistory(AutopilotConversation conversation) {
        List<AutopilotMessage> messages = conversation.getMessages();
        if (messages.size() > MAX_HISTORY_MESSAGES) {
            conversation.setMessages(new ArrayList<>(
                    messages.subList(messages.size() - MAX_HISTORY_MESSAGES, messages.size())));
        }
    }

    private List<Map<String, String>> buildChatMessages(String systemInstruction, List<AutopilotMessage> history) {
        List<Map<String, String>> out = new ArrayList<>();
        out.add(Map.of("role", "system", "content", systemInstruction));
        for (AutopilotMessage m : history) {
            String role = "assistant".equals(m.getRole()) ? "assistant" : "user";
            out.add(Map.of("role", role, "content", m.getContent() != null ? m.getContent() : ""));
        }
        return out;
    }

    private String buildSystemInstruction(AutopilotConfig config, AiFaqConfig knowledge,
                                           AllowedActions a, List<Automation> triggerableAutomations) {
        StringBuilder sb = new StringBuilder();

        sb.append(roleTemplate(config.getRole()));
        sb.append("\nTone: ").append(config.getTone() != null && !config.getTone().isBlank() ? config.getTone() : "friendly").append(".\n");

        if (config.getGoal() != null && !config.getGoal().isBlank()) {
            sb.append("Your goal for this conversation: ").append(config.getGoal().trim()).append("\n");
        }
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            sb.append("\n--- BUSINESS INSTRUCTIONS ---\n").append(config.getSystemPrompt().trim()).append('\n');
        }

        if (knowledge != null && knowledge.getQaPairs() != null && !knowledge.getQaPairs().isEmpty()) {
            sb.append("\n--- CURATED Q&A (use these first if relevant) ---\n");
            knowledge.getQaPairs().forEach(p -> {
                if (p.getQuestion() == null || p.getQuestion().isBlank()) return;
                sb.append("Q: ").append(p.getQuestion().trim()).append('\n');
                sb.append("A: ").append(p.getAnswer() != null ? p.getAnswer().trim() : "").append("\n\n");
            });
        }
        if (knowledge != null && knowledge.getKnowledgeBase() != null && !knowledge.getKnowledgeBase().isBlank()) {
            sb.append("\n--- BUSINESS INFO / PRODUCTS ---\n").append(knowledge.getKnowledgeBase().trim()).append('\n');
        }

        sb.append("\n--- WHAT YOU MAY DO ---\n");
        sb.append("- Ask natural follow-up questions to move the conversation toward your goal.\n");
        if (a.getCollectEmail()) sb.append("- You may ask for and collect the customer's email address.\n");
        if (a.getCollectPhone()) sb.append("- You may ask for and collect the customer's phone number.\n");
        if (a.getRecommendProducts()) sb.append("- You may recommend products/services from the business info above.\n");
        sb.append("- Never invent facts, prices, or policies not stated above.\n");
        sb.append("- Keep replies short and conversational (under 500 characters), like a real DM, not an email.\n");

        boolean hasTemplates = a.getSendTemplates() && config.getMessageTemplates() != null
                && !config.getMessageTemplates().isEmpty();
        if (hasTemplates) {
            sb.append("\n--- PREDEFINED MESSAGE TEMPLATES YOU MAY SEND VERBATIM ---\n");
            sb.append("When one of these clearly matches what the customer wants, send it using the action ")
                    .append("field below instead of writing your own text for it (their \"reply\" text will be ")
                    .append("replaced with the template automatically).\n");
            for (MessageTemplate t : config.getMessageTemplates()) {
                sb.append("id=\"").append(t.getId()).append("\" — ").append(t.getLabel());
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    sb.append(" (").append(t.getDescription().trim()).append(")");
                }
                sb.append('\n');
            }
        }

        boolean hasAutomations = a.getTriggerAutomations() && !triggerableAutomations.isEmpty();
        if (hasAutomations) {
            sb.append("\n--- EXISTING AUTOMATIONS YOU MAY TRIGGER ---\n");
            sb.append("Use these to hand off to an existing workflow (e.g. a booking link, a tagged follow-up ")
                    .append("sequence) when appropriate.\n");
            for (Automation auto : triggerableAutomations) {
                sb.append("id=\"").append(auto.getId()).append("\" — ")
                        .append(auto.getName() != null ? auto.getName() : "Untitled automation").append('\n');
            }
        }

        sb.append("\n--- OUTPUT FORMAT (STRICT) ---\n");
        sb.append("""
                Respond with ONLY a single JSON object, no markdown, no code fences, no extra text:
                {
                  "reply": "the message to send the customer",
                  "collected": {"name": null, "email": null, "phone": null, "preferences": null, "budget": null},
                  "tags": [],
                  "qualified": false,
                  "escalate": false,
                  "action": {"type": "none", "id": null}
                }
                Rules for the JSON fields:
                - "collected": only include a field's value if the customer stated it in THIS message; otherwise use null.
                - "qualified": true only once the customer clearly matches the goal above (e.g. ready to buy, meets criteria).
                - "escalate": true only if you cannot confidently help, the customer is upset, or explicitly asks for a human.
                - "tags": short lowercase keywords describing this lead (e.g. "interested", "price-sensitive"), or [].
                - "action.type": "none", or "send_template" with the template's id above, or "trigger_automation" \
                with the automation's id above. Only use ids that were explicitly listed to you. Leave as "none" \
                unless one clearly applies.
                """);

        return sb.toString();
    }

    private String roleTemplate(com.creatorengine.autopilot.entity.AutopilotRole role) {
        if (role == null) role = com.creatorengine.autopilot.entity.AutopilotRole.SALES_ASSISTANT;
        return switch (role) {
            case SALES_ASSISTANT -> "You are an AI sales assistant for an Instagram business account, "
                    + "having a DM conversation with a potential customer. Your job is to understand what "
                    + "they need, answer questions, and move them toward a purchase.";
            case CUSTOMER_SUPPORT -> "You are an AI customer support agent for an Instagram business account, "
                    + "having a DM conversation with a customer. Your job is to resolve their issue or question "
                    + "as helpfully as possible.";
            case COACH -> "You are an AI coach/consultant assistant for an Instagram business account, "
                    + "having a DM conversation with a prospective client. Your job is to understand their "
                    + "situation and goals, and guide them toward booking a call or program.";
            case CUSTOM -> "You are an AI assistant for an Instagram business account, having a DM "
                    + "conversation on the business's behalf. Follow the business instructions below closely.";
        };
    }

    private record AutopilotTurn(
            String reply, Map<String, String> collected, List<String> tags, boolean qualified, boolean escalate,
            String actionType, String actionId
    ) {}

    private AutopilotTurn parseTurn(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = extractJsonObject(raw);

        try {
            JsonNode root = json.readTree(cleaned);
            String reply = root.path("reply").isTextual() ? root.path("reply").asText() : null;

            Map<String, String> collected = new LinkedHashMap<>();
            JsonNode collectedNode = root.path("collected");
            if (collectedNode.isObject()) {
                collectedNode.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isTextual() && !entry.getValue().asText().isBlank()) {
                        collected.put(entry.getKey(), entry.getValue().asText().trim());
                    }
                });
            }

            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = root.path("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(t -> {
                    if (t.isTextual() && !t.asText().isBlank()) tags.add(t.asText().trim().toLowerCase());
                });
            }

            boolean qualified = root.path("qualified").asBoolean(false);
            boolean escalate = root.path("escalate").asBoolean(false);

            JsonNode actionNode = root.path("action");
            String actionType = actionNode.path("type").isTextual() ? actionNode.path("type").asText() : "none";
            String actionId = actionNode.path("id").isTextual() && !actionNode.path("id").asText().isBlank()
                    ? actionNode.path("id").asText().trim() : null;
            if ("none".equals(actionType)) actionId = null;

            if (reply == null || reply.isBlank()) {
                // Model didn't follow the JSON contract — fall back to raw text as the reply.
                return new AutopilotTurn(raw.trim(), collected, tags, qualified, escalate, actionType, actionId);
            }
            return new AutopilotTurn(reply.trim(), collected, tags, qualified, escalate, actionType, actionId);
        } catch (Exception ex) {
            // Not valid JSON at all — treat the whole response as the reply text.
            log.debug("Autopilot: response wasn't valid JSON, using raw text as reply: {}", ex.getMessage());
            return new AutopilotTurn(raw.trim(), Map.of(), List.of(), false, false, "none", null);
        }
    }

    /** Models sometimes wrap JSON in ```fences``` or add stray text — pull out the {...} block. */
    private String extractJsonObject(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }

    private void applyCollectedData(AutopilotConversation conversation, AutopilotTurn turn, AllowedActions actions) {
        Map<String, String> collected = turn.collected();
        if (collected == null || collected.isEmpty()) return;

        if (collected.get("name") != null) conversation.setCollectedName(collected.get("name"));
        if (actions.getCollectEmail() && collected.get("email") != null) conversation.setCollectedEmail(collected.get("email"));
        if (actions.getCollectPhone() && collected.get("phone") != null) conversation.setCollectedPhone(collected.get("phone"));
        if (collected.get("preferences") != null) conversation.setCollectedPreferences(collected.get("preferences"));
        if (collected.get("budget") != null) conversation.setCollectedBudget(collected.get("budget"));
    }

    private void syncContact(String uid, String igAccountId, AutopilotConversation conversation,
                              AutopilotTurn turn, AllowedActions actions) {
        if (!actions.getUpdateContacts()) return;

        Map<String, Object> updates = new HashMap<>();
        if (conversation.getCollectedName() != null) updates.put("name", conversation.getCollectedName());
        if (conversation.getCollectedEmail() != null) updates.put("email", conversation.getCollectedEmail());
        if (conversation.getCollectedPhone() != null) updates.put("phone", conversation.getCollectedPhone());
        if (conversation.getCollectedPreferences() != null) updates.put("preferences", conversation.getCollectedPreferences());
        if (conversation.getCollectedBudget() != null) updates.put("budget", conversation.getCollectedBudget());
        if (conversation.getQualified()) updates.put("qualified", true);

        if (!updates.isEmpty()) {
            try {
                contactRepository.patchFields(uid, igAccountId, conversation.getInstagramUserId(), updates);
            } catch (Exception ex) {
                log.warn("Autopilot: failed to sync contact fields uid={}: {}", uid, ex.getMessage());
            }
        }

        if (actions.getAddTags() && turn.tags() != null) {
            for (String tag : turn.tags()) {
                try {
                    contactRepository.addTag(uid, igAccountId, conversation.getInstagramUserId(), tag);
                } catch (Exception ex) {
                    log.warn("Autopilot: failed to add tag '{}' uid={}: {}", tag, uid, ex.getMessage());
                }
            }
        }
    }

    private void notifyOwner(String uid, AutopilotConversation conversation, boolean escalated) {
        try {
            userRepository.findById(uid).ifPresent(user -> {
                String toEmail = user.getEmail();
                if (toEmail == null || toEmail.isBlank()) return;

                String subject = escalated
                        ? "Autopilot escalated a conversation to you"
                        : "Autopilot qualified a new lead";
                String who = conversation.getUsername() != null ? "@" + conversation.getUsername() : "A contact";
                String body = escalated
                        ? who + " needs your attention — AI Autopilot couldn't confidently continue the conversation. "
                            + "Open your Instagram DMs to follow up."
                        : who + " was just qualified as a lead by AI Autopilot. Check their contact profile for details.";

                emailService.sendOwnerNotification(toEmail, subject, body);
            });
        } catch (Exception ex) {
            log.warn("Autopilot: owner notification failed uid={}: {}", uid, ex.getMessage());
        }
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
