package com.creatorengine.automation.queue;

import com.creatorengine.instagram.dto.WebhookEventDto;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

/**
 * Unit of work in the automation queue.
 *
 * <p>Granularity: one job per (uid, event, automationId) triple — not
 * per event. If a single comment matches three automations, three jobs
 * are enqueued. This way a retryable failure on automation A doesn't
 * cause already-succeeded automations B and C to fire again.</p>
 *
 * <p>{@link #event} is a snapshot at enqueue time; {@link #automationId}
 * is just a pointer — the worker re-fetches the automation when it
 * processes the job so that toggle-off or deletion takes effect even
 * mid-flight.</p>
 *
 * <p>{@link #attempt} is 1 on first try and incremented by the retry
 * machinery. {@link #lastError} carries forward the most recent
 * failure reason for debugging dead-lettered jobs.</p>
 *
 * <p>{@link #actionIndex} is the next action to execute within the
 * automation's sequential chain. Zero on first dispatch; advanced
 * across DELAY continuations and stays put across retries. Old jobs
 * persisted before multi-step support deserialise as 0, which means
 * "start from the beginning" — exactly what we want.</p>
 *
 * <p>Immutability + {@code @With} lets the retry path produce a new
 * job instance ergonomically: {@code job.withAttempt(2).withLastError(...)}.</p>
 */
@Builder
public record AutomationJob(
        @With String jobId,
        @With String uid,
        @With WebhookEventDto event,
        @With String automationId,
        @With int attempt,
        @With int actionIndex,
        @With Instant enqueuedAt,
        @With String lastError
) {
    public static AutomationJob fresh(String uid, WebhookEventDto event, String automationId) {
        return AutomationJob.builder()
                .jobId(UUID.randomUUID().toString())
                .uid(uid)
                .event(event)
                .automationId(automationId)
                .attempt(1)
                .actionIndex(0)
                .enqueuedAt(Instant.now())
                .build();
    }

    /** Convenience for retries — keep the same job id so logs correlate. */
    public AutomationJob nextAttempt(String error) {
        return this
                .withAttempt(this.attempt + 1)
                .withLastError(error)
                .withEnqueuedAt(Instant.now());
    }

    /**
     * Advance to the action after {@code currentIndex}, resetting the
     * retry budget — each action gets its own three-strike attempt count.
     */
    public AutomationJob advanceTo(int nextActionIndex) {
        return this
                .withActionIndex(nextActionIndex)
                .withAttempt(1)
                .withLastError(null)
                .withEnqueuedAt(Instant.now());
    }
}
