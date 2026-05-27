package com.creatorengine.automation.deadletter;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.queue.AutomationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    private final FailedJobRepository repository;

    public DeadLetterService(FailedJobRepository repository) {
        this.repository = repository;
    }

    public void record(AutomationJob job, String reason) {
        record(job, null, reason);
    }

    public void record(AutomationJob job, Automation automation, String reason) {
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