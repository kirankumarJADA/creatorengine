package com.creatorengine.instagram.service;

import com.creatorengine.automation.engine.AutomationEngine;
import com.creatorengine.config.AppProperties;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.WebhookEventRecord;
import com.creatorengine.instagram.repository.InstagramAccountRepository.OwnedAccount;
import com.creatorengine.instagram.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Top-level webhook orchestration.
 *
 * <p>The controller hands us the raw body + signature; we own:</p>
 * <ol>
 *   <li>Signature verification (skipped in dev when app secret is blank).</li>
 *   <li>Parsing the payload into normalised {@link WebhookEventDto}s.</li>
 *   <li>Attributing each event to a CreatorEngine user via the IG account id.</li>
 *   <li>Persisting events (user subcollection vs. orphan bucket).</li>
 *   <li>Touching the connected account's {@code lastSyncAt}.</li>
 * </ol>
 *
 * <p>Per spec, this is the end of the road for the MVP — we do NOT
 * trigger automations or send DMs from here yet. Those plug in later
 * as the next pipeline stage.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final AppProperties props;
    private final MetaSignatureVerifier signatureVerifier;
    private final WebhookEventParser parser;
    private final InstagramAccountService accountService;
    private final WebhookEventRepository eventRepository;
    private final AutomationEngine automationEngine;
    private final com.creatorengine.health.HealthService healthService;
    private final ObjectMapper objectMapper;

    // ─── Verification handshake ──────────────────────────────
    /** True when {@code hub.mode == "subscribe"} and the verify-token matches ours. */
    public boolean isValidVerification(String mode, String token) {
        return "subscribe".equals(mode)
                && token != null
                && token.equals(props.getMeta().getVerifyToken());
    }

    // ─── Event delivery ──────────────────────────────────────
    /**
     * Entry point from the controller. Takes raw bytes because Meta signs
     * the exact bytes on the wire — any String round-trip (charset decode +
     * re-encode) breaks HMAC verification for payloads with non-ASCII content
     * such as emoji in IG comments or accented usernames.
     */
    public ProcessingResult processIncoming(String signatureHeader, byte[] rawBody) {
        // 1. Verify signature. In production this is non-negotiable; in
        //    dev we accept unsigned payloads when the secret is blank so
        //    you can curl the endpoint without setting Meta up.
        boolean hasSecret = props.getMeta().getAppSecret() != null
                && !props.getMeta().getAppSecret().isBlank();
        if (hasSecret && !signatureVerifier.isValid(signatureHeader, rawBody)) {
            log.warn("Webhook rejected — invalid X-Hub-Signature-256.");
            return new ProcessingResult(false, 0, 0, "Invalid signature.");
        }
        // Mark for the health endpoint — only signed/accepted POSTs count.
        healthService.markWebhookHit();

        // Signature already verified on raw bytes — safe to decode for JSON parsing.
        // Meta always sends UTF-8 JSON; this decode is lossless and never feeds
        // back into HMAC.
        String bodyJson = rawBody == null ? "" : new String(rawBody, StandardCharsets.UTF_8);
        return parseAndDispatch(bodyJson);
    }

    /**
     * Dev/test path: skip signature verification entirely and run the
     * payload through the parser + dispatcher. Used by the dev-profile
     * webhook simulator and unit tests; <b>never</b> wired to a
     * Meta-facing endpoint. Stays on {@code String} because callers
     * already have decoded text and there's no HMAC to protect.
     */
    public ProcessingResult processUnsigned(String rawBody) {
        return parseAndDispatch(rawBody);
    }

    private ProcessingResult parseAndDispatch(String rawBody) {
        List<WebhookEventDto> events = parser.parse(rawBody);
        log.info("Webhook payload parsed: {} event(s).", events.size());

        int attributed = 0, orphaned = 0;
        for (WebhookEventDto e : events) {
            if (dispatch(e, rawBody)) attributed++;
            else                      orphaned++;
        }
        return new ProcessingResult(true, attributed, orphaned, null);
    }

    /**
     * Persist a single event, attributing to a user when possible.
     * @return true when the event was attributed to a user
     */
    public boolean dispatch(WebhookEventDto e, String rawPayloadHint) {
        WebhookEventRecord record = toRecord(e, rawPayloadHint);

        if (e.receivingAccountId() == null) {
            log.warn("Event with no receivingAccountId — storing as orphan.");
            eventRepository.saveOrphan(record);
            return false;
        }

        Optional<OwnedAccount> owner = accountService.findByInstagramUserId(e.receivingAccountId());
        if (owner.isEmpty()) {
            log.debug("No CreatorEngine user owns IG account {} — storing as orphan.",
                    e.receivingAccountId());
            eventRepository.saveOrphan(record);
            return false;
        }

        String uid = owner.get().uid();
        eventRepository.saveForUser(uid, record);
        accountService.touchLastSync(uid);

        // Hook for the next stage (automation engine). Intentionally a
        // no-op for the MVP — see spec: "DO NOT trigger automations yet".
        deferAutomationDispatch(uid, e);
        return true;
    }

    // ─── Mapping ─────────────────────────────────────────────
    private WebhookEventRecord toRecord(WebhookEventDto e, String rawPayloadHint) {
        Map<String, Object> rawSnapshot = sliceRaw(rawPayloadHint, e);
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

    /**
     * Best-effort slice of the original payload so the stored record
     * contains the JSON shape Meta sent. We store the whole root —
     * cheap, debuggable, easy to drop later if storage cost matters.
     */
    private Map<String, Object> sliceRaw(String rawPayloadHint, WebhookEventDto e) {
        if (rawPayloadHint == null || rawPayloadHint.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(rawPayloadHint);
            return objectMapper.convertValue(root, Map.class);
        } catch (Exception ex) {
            log.debug("Could not snapshot raw payload: {}", ex.getMessage());
            return null;
        }
    }

    // ─── Hand off to the automation engine ───────────────────
    /**
     * Where the automation engine plugs in.
     *
     * <p>{@code engine.dispatch} is synchronous but does only matching,
     * gating, and enqueueing — no Meta API calls. Actual sends happen
     * on {@link com.creatorengine.automation.queue.QueueWorker} threads
     * via {@code engine.processJob}, so this call returns to the
     * webhook handler in tens of milliseconds even under load.</p>
     */
    private void deferAutomationDispatch(String uid, WebhookEventDto event) {
        automationEngine.dispatch(uid, event);
    }

    /** Result of a single webhook POST — used for the controller response + tests. */
    public record ProcessingResult(boolean accepted, int attributed, int orphaned, String error) {}
}