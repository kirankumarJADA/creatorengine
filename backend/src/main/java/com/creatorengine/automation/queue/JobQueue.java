package com.creatorengine.automation.queue;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over the automation work queue.
 *
 * <p>Implementations live in the same package: {@link InMemoryJobQueue}
 * is the current default. The interface is intentionally minimal so
 * a Redis/SQS-backed implementation can land later without ripple
 * effects in the engine.</p>
 *
 * <p>Two enqueue paths exist:</p>
 * <ul>
 *   <li>{@link #enqueue} — immediate, for first-attempt dispatches.</li>
 *   <li>{@link #enqueueDelayed} — scheduled, for retries / rate-limited
 *       re-tries / cooldown deferrals.</li>
 * </ul>
 */
public interface JobQueue {

    /** Add a job for immediate processing. Never blocks. */
    void enqueue(AutomationJob job);

    /**
     * Schedule a job to become available after {@code delay}.
     * Implementations should not block the caller.
     */
    void enqueueDelayed(AutomationJob job, Duration delay);

    /**
     * Block until a job is available or the calling thread is interrupted.
     * Used exclusively by {@link QueueWorker}.
     */
    Optional<AutomationJob> poll(Duration timeout) throws InterruptedException;

    /** Approximate queue depth — for the health endpoint. */
    int size();
}
