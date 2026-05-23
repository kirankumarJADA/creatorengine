package com.creatorengine.automation.deadletter;

import com.creatorengine.automation.queue.AutomationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Thin wrapper that takes a {@link AutomationJob} and the reason it
 * gave up, and persists a {@link FailedJob}. Lives in front of the
 * repository so the worker doesn't depend on Firestore directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final FailedJobRepository repository;

    public void record(AutomationJob job, String reason) {
        record(job, null, reason);
    }

    /**
     * Same as {@link #record(AutomationJob, String)} but also stamps the
     * automation's snapshot name into the persisted record. The engine
     * uses this overload because it already has the automation in hand
     * by the time a job dead-letters.
     */
    public void record(
            AutomationJob job,
            com.creatorengine.automation.entity.Automation automation,
            String reason
    ) {
        FailedJob row = FailedJob.builder()
                .eventId(job.event() != null ? job.event().dedupKey() : null)
                .automationId(job.automationId())
                .automationName(automation != null ? automation.getName() : null)
                .username(job.event() != null ? job.event().username() : null)
                .reason(reason)
                .attempts(job.attempt())
                .jobId(job.jobId())
                .createdAt(Instant.now())
                .event(WebhookEventSnapshot.fromDto(job.event()))
                .build();
        repository.save(job.uid(), row);
        log.warn("Dead-lettered job={} automation={} attempts={} reason={}",
                job.jobId(),
                automation != null ? automation.getId() : job.automationId(),
                job.attempt(),
                reason);
    }
}
