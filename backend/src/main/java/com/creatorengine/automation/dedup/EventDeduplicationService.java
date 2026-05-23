package com.creatorengine.automation.dedup;

import com.creatorengine.instagram.dto.WebhookEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Single source of truth for "have we already processed this event?".
 *
 * <p>Use exactly once per inbound event, at the earliest sensible
 * point in the pipeline (today: just before fanout in
 * {@code AutomationEngine.dispatch}). The check + mark is not strictly
 * atomic — under tight redelivery races we could process an event
 * twice. The trade-off is intentional: a strict CAS would need a
 * Firestore transaction per event, which costs more than the
 * occasional duplicate DM is worth at this scale.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventDeduplicationService {

    /** How long a processed-event record sticks around. Set Firestore TTL accordingly. */
    private static final Duration RETENTION = Duration.ofDays(7);

    private final ProcessedEventRepository repository;

    /** True if this event has already been seen and should be skipped. */
    public boolean isDuplicate(WebhookEventDto event) {
        String key = event.dedupKey();
        if (key == null) {
            // No usable id — can't dedup. Caller decides whether to proceed.
            return false;
        }
        return repository.exists(key);
    }

    /** Mark this event as processed. Best-effort — never throws. */
    public void markProcessed(String uid, WebhookEventDto event) {
        String key = event.dedupKey();
        if (key == null) return;

        Instant now = Instant.now();
        ProcessedEvent record = ProcessedEvent.builder()
                .id(key)
                .eventType(event.type() != null ? event.type().name() : null)
                .uid(uid)
                .processedAt(now)
                .expiresAt(now.plus(RETENTION))
                .build();
        repository.save(record);
        log.debug("Recorded processed event key={} uid={}", key, uid);
    }

    /**
     * Combined "check + mark" used by the engine: returns true when the
     * event is brand new (and was just marked), false when it had
     * already been seen (caller should skip it).
     *
     * <p>This is the single call the engine makes per inbound event.</p>
     */
    public boolean markIfNew(String uid, WebhookEventDto event) {
        if (isDuplicate(event)) {
            log.info("Duplicate event ignored key={} uid={}", event.dedupKey(), uid);
            return false;
        }
        markProcessed(uid, event);
        return true;
    }
}
