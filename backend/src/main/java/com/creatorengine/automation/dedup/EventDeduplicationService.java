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

    public boolean isDuplicate(WebhookEventDto event) {
        String key = event.dedupKey();
        if (key == null) {
            return false;
        }
        return repository.exists(key);
    }

    public void markProcessed(String uid, WebhookEventDto event) {
        String key = event.dedupKey();
        if (key == null) {
            return;
        }

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

    public boolean markIfNew(String uid, WebhookEventDto event) {
        if (isDuplicate(event)) {
            log.info("Duplicate event ignored key={} uid={}", event.dedupKey(), uid);
            return false;
        }

        markProcessed(uid, event);
        return true;
    }
}