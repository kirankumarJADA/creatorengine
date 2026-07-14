package com.creatorengine.instagram.service;

import com.creatorengine.automation.email.EmailCollectionService;
import com.creatorengine.automation.engine.AutomationEngine;
import com.creatorengine.automation.followup.FollowUpService;
import com.creatorengine.config.AppProperties;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.entity.WebhookEventRecord;
import com.creatorengine.instagram.repository.InstagramAccountRepository.OwnedAccount;
import com.creatorengine.instagram.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final AppProperties props;
    private final MetaSignatureVerifier signatureVerifier;
    private final WebhookEventParser parser;
    private final InstagramAccountService accountService;
    private final WebhookEventRepository eventRepository;
    private final AutomationEngine automationEngine;
    private final com.creatorengine.health.HealthService healthService;
    private final ObjectMapper objectMapper;
    private final FollowUpService followUpService;
    private final EmailCollectionService emailCollectionService;
    private final MetaMessagingService metaMessagingService;

    public WebhookService(
            AppProperties props,
            MetaSignatureVerifier signatureVerifier,
            WebhookEventParser parser,
            InstagramAccountService accountService,
            WebhookEventRepository eventRepository,
            AutomationEngine automationEngine,
            com.creatorengine.health.HealthService healthService,
            ObjectMapper objectMapper,
            FollowUpService followUpService,
            EmailCollectionService emailCollectionService,
            MetaMessagingService metaMessagingService
    ) {
        this.props = props;
        this.signatureVerifier = signatureVerifier;
        this.parser = parser;
        this.accountService = accountService;
        this.eventRepository = eventRepository;
        this.automationEngine = automationEngine;
        this.healthService = healthService;
        this.objectMapper = objectMapper;
        this.followUpService = followUpService;
        this.emailCollectionService = emailCollectionService;
        this.metaMessagingService = metaMessagingService;
    }

    public boolean isValidVerification(String mode, String token) {
        return "subscribe".equals(mode)
                && token != null
                && token.equals(props.getMeta().getVerifyToken());
    }

    public ProcessingResult processIncoming(String signatureHeader, byte[] rawBody) {
        boolean hasSecret = props.getMeta().getAppSecret() != null
                && !props.getMeta().getAppSecret().isBlank();

        if (hasSecret && !signatureVerifier.isValid(signatureHeader, rawBody)) {
            log.warn("Webhook rejected - invalid X-Hub-Signature-256.");
            return new ProcessingResult(false, 0, 0, "Invalid signature.");
        }

        healthService.markWebhookHit();

        String bodyJson = rawBody == null ? "" : new String(rawBody, StandardCharsets.UTF_8);
        return parseAndDispatch(bodyJson);
    }

    public ProcessingResult processUnsigned(String rawBody) {
        return parseAndDispatch(rawBody);
    }

    private ProcessingResult parseAndDispatch(String rawBody) {
        List<WebhookEventDto> events = parser.parse(rawBody);
        log.info("Webhook payload parsed: {} event(s).", events.size());

        int attributed = 0;
        int orphaned = 0;

        for (WebhookEventDto e : events) {
            if (dispatch(e, rawBody)) {
                attributed++;
            } else {
                orphaned++;
            }
        }

        return new ProcessingResult(true, attributed, orphaned, null);
    }

    public boolean dispatch(WebhookEventDto e, String rawPayloadHint) {
        WebhookEventRecord record = toRecord(e, rawPayloadHint);

        if (e.receivingAccountId() == null) {
            log.warn("Event with no receivingAccountId - storing as orphan.");
            eventRepository.saveOrphan(record);
            return false;
        }

        Optional<OwnedAccount> owner = accountService.findByInstagramUserId(e.receivingAccountId());

        if (owner.isEmpty()) {
            log.debug("No CreatorEngine user owns IG account {} - storing as orphan.",
                    e.receivingAccountId());
            eventRepository.saveOrphan(record);
            return false;
        }

        String uid = owner.get().uid();
        InstagramAccount account = owner.get().account();

        if ((e.type() == EventType.COMMENT || e.type() == EventType.LIVE_COMMENT)
                && isOwnComment(e, account)) {
            log.info("Skipping self-authored comment (own public reply or owner comment): commentId={} from={}",
                    e.commentId(), e.username());
            return false;
        }

        // STORY MENTION: resolve the sender's Instagram user id from the media_id.
        // The parser can't do this — it doesn't have the account's access token.
        if (e.type() == EventType.STORY_MENTION) {
            var mentionOwnerOpt = metaMessagingService.resolveMediaOwner(e.postId(), account.getAccessToken());
            if (mentionOwnerOpt.isEmpty()) {
                log.warn("Could not resolve story mention sender for mediaId={} - dropping event.", e.postId());
                return false;
            }
            var mentionOwner = mentionOwnerOpt.get();
            // Skip if the mention came from the owner's own account
            if (mentionOwner.id() != null && mentionOwner.id().equals(account.getInstagramUserId())) {
                log.debug("Skipping story mention authored by the owning account.");
                return false;
            }
            e = WebhookEventDto.builder()
                    .type(EventType.STORY_MENTION)
                    .instagramUserId(mentionOwner.id())
                    .username(mentionOwner.username())
                    .postId(e.postId())
                    .eventTime(e.eventTime())
                    .receivingAccountId(e.receivingAccountId())
                    .build();
            log.info("STORY_MENTION enriched: sender={} ({})", mentionOwner.id(), mentionOwner.username());
        }

        // ----------------------------------------------------------------
        // FOLLOW-UP MESSAGE: cancel-on-reply
        // Any incoming DM from a contact means they replied - cancel any
        // pending no-reply follow-up(s) scheduled for them under this
        // account, regardless of which automation scheduled it.
        // ----------------------------------------------------------------
        if ((e.type() == EventType.DM
                || e.type() == EventType.CONTENT_SHARED
                || e.type() == EventType.STORY_MENTION)
                && e.instagramUserId() != null) {
            followUpService.cancelPendingForUser(uid, e.instagramUserId());

            // EMAIL COLLECTION: try to capture email from the reply
            emailCollectionService.tryCapture(uid, e.instagramUserId(), e.message(), e.receivingAccountId());
        }

        eventRepository.saveForUser(uid, record);
        accountService.touchLastSync(uid);
        deferAutomationDispatch(uid, e);

        return true;
    }

    private boolean isOwnComment(WebhookEventDto e, InstagramAccount account) {
        if (account == null) return false;

        String ownerUsername = account.getUsername();
        String ownerIgUserId = account.getInstagramUserId();

        String eventUsername = e.username();
        String eventUserId = e.instagramUserId();

        if (ownerUsername != null && eventUsername != null
                && ownerUsername.equalsIgnoreCase(eventUsername)) {
            return true;
        }

        if (ownerIgUserId != null && eventUserId != null
                && ownerIgUserId.equals(eventUserId)) {
            return true;
        }

        return false;
    }

    private WebhookEventRecord toRecord(WebhookEventDto e, String rawPayloadHint) {
        Map<String, Object> rawSnapshot = sliceRaw(rawPayloadHint);

        return WebhookEventRecord.builder()
                .type(e.type())
                .message(e.message())
                .username(e.username())
                .instagramUserId(e.instagramUserId())
                .postId(e.postId())
                .commentId(e.commentId())
                .messageId(e.messageId())
                .eventTime(e.eventTime())
                .receivedAt(Instant.now())
                .rawPayload(rawSnapshot)
                .build();
    }

    private Map<String, Object> sliceRaw(String rawPayloadHint) {
        if (rawPayloadHint == null || rawPayloadHint.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayloadHint);
            return objectMapper.convertValue(root, Map.class);
        } catch (Exception ex) {
            log.debug("Could not snapshot raw payload: {}", ex.getMessage());
            return null;
        }
    }

    private void deferAutomationDispatch(String uid, WebhookEventDto event) {
        automationEngine.dispatch(uid, event);
    }

    public record ProcessingResult(boolean accepted, int attributed, int orphaned, String error) {
    }
}