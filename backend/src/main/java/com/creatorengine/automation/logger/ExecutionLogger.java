package com.creatorengine.automation.logger;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ExecutionLog;
import com.creatorengine.automation.executor.ExecutionResult;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Writes one Firestore document per notable engine outcome.
 *
 * <p>Three log paths exist:</p>
 * <ul>
 *   <li>{@link #logMatch}            — an automation actually fired
 *                                      (success or terminal failure).</li>
 *   <li>{@link #logCooldownSkipped}  — a match was suppressed by the
 *                                      anti-spam cooldown.</li>
 *   <li>{@link #logDuplicateIgnored} — a redelivered Meta event was
 *                                      short-circuited by the dedup layer.</li>
 * </ul>
 *
 * <p>Logging is wrapped in a broad try/catch because a logging failure
 * must never abort an actual execution (i.e. drop the DM that already
 * went out the door).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionLogger {

    // Status values surfaced by the Activity Logs API. Kept as plain
    // strings rather than an enum so old rows (which lack the field)
    // deserialise as null without ClassCastException pain.
    public static final String STATUS_SUCCESS           = "SUCCESS";
    public static final String STATUS_FAILED            = "FAILED";
    public static final String STATUS_COOLDOWN_SKIPPED  = "COOLDOWN_SKIPPED";
    public static final String STATUS_DUPLICATE_IGNORED = "DUPLICATE_IGNORED";

    private final ExecutionLogRepository repository;

    // ─── Match (success or terminal failure) ─────────────────
    public void logMatch(
            String uid,
            Automation automation,
            WebhookEventDto event,
            ExecutionResult result
    ) {
        String status = deriveMatchStatus(result);
        ExecutionLog row = ExecutionLog.builder()
                .automationId(automation.getId())
                .automationName(automation.getName())
                .matched(true)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(result.messageSent())
                .status(status)
                .renderedMessage(result.renderedMessage())
                .actionType(automation.getAction() != null && automation.getAction().getType() != null
                        ? automation.getAction().getType().name()
                        : null)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .metaMessageId(result.metaMessageId())
                .errorMessage(result.error())
                .timestamp(Instant.now())
                .build();
        safeSave(uid, row, "match");
    }

    // ─── Cooldown skipped ───────────────────────────────────
    /**
     * Recorded when {@link com.creatorengine.automation.cooldown.CooldownService}
     * suppresses a would-be match. {@code matched} is still true
     * (the automation's trigger + condition both matched the event),
     * but {@code messageSent} is false — no DM went out.
     */
    public void logCooldownSkipped(String uid, Automation automation, WebhookEventDto event) {
        ExecutionLog row = ExecutionLog.builder()
                .automationId(automation.getId())
                .automationName(automation.getName())
                .matched(true)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(false)
                .status(STATUS_COOLDOWN_SKIPPED)
                .actionType(automation.getAction() != null && automation.getAction().getType() != null
                        ? automation.getAction().getType().name()
                        : null)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .timestamp(Instant.now())
                .build();
        safeSave(uid, row, "cooldown-skip");
    }

    // ─── Duplicate ignored ──────────────────────────────────
    /**
     * Recorded when the dedup layer short-circuits a redelivered Meta
     * event. There's no automation in scope at this point (dedup
     * happens before matching), so {@code automationId} /
     * {@code automationName} stay null and {@code matched} is false —
     * we genuinely didn't match anything because we didn't look.
     */
    public void logDuplicateIgnored(String uid, WebhookEventDto event) {
        ExecutionLog row = ExecutionLog.builder()
                .matched(false)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(false)
                .status(STATUS_DUPLICATE_IGNORED)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .timestamp(Instant.now())
                .build();
        safeSave(uid, row, "dup-ignored");
    }

    // ─── Helpers ────────────────────────────────────────────
    private void safeSave(String uid, ExecutionLog row, String tag) {
        try {
            repository.save(uid, row);
            log.debug("Logged [{}] uid={} automation={} status={}",
                    tag, uid, row.getAutomationId(), row.getStatus());
        } catch (Exception ex) {
            log.warn("Failed to persist [{}] log uid={}: {}", tag, uid, ex.getMessage());
        }
    }

    private static String deriveMatchStatus(ExecutionResult result) {
        if (result == null) return STATUS_FAILED;
        if (result.messageSent()) return STATUS_SUCCESS;
        // SAVE_CONTACT succeeds without sending anything — treat as success.
        if (result.error() == null) return STATUS_SUCCESS;
        return STATUS_FAILED;
    }
}
