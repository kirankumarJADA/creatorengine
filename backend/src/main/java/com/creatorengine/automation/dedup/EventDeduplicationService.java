package com.creatorengine.automation.dedup;

import com.creatorengine.instagram.dto.WebhookEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class EventDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicationService.class);

    private static final Duration RETENTION = Duration.ofDays(7);

    private final ProcessedEventRepository repository;

    public EventDeduplicationService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Atomically check-and-claim this event.
     * Returns true if this is the first time we've seen this event (caller should process it).
     * Returns false if another request already claimed it (caller must skip it).
     *
     * Replaces the old isDuplicate() + markProcessed() two-step which had a
     * race condition when Meta retried a webhook during a cold-start delay.
     */
    public boolean markIfNew(String uid, WebhookEventDto event) {
        String key = event.dedupKey();
        if (key == null) {
            // No dedup key (unknown event type) — allow through
            return true;
        }

        Instant now = Instant.now();
        ProcessedEvent record = ProcessedEvent.builder()
                .id(key)
                .eventType(event.type() != null ? event.type().name() : null)
                .uid(uid)
                .processedAt(now)
                .expiresAt(now.plus(RETENTION))
                .build();

        boolean claimed = repository.tryClaimAndSave(record);
        if (!claimed) {
            log.info("Duplicate event ignored key={} uid={}", key, uid);
        }
        return claimed;
    }
}