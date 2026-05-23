package com.creatorengine.automation.queue;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory {@link JobQueue} backed by a {@link LinkedBlockingQueue}
 * for immediate jobs and a {@link ScheduledExecutorService} for
 * delayed/retry jobs.
 *
 * <p><b>Production caveat</b>: the queue lives entirely in the JVM
 * heap. Crashes or restarts lose any in-flight jobs. That's an
 * acceptable trade-off for the MVP (Meta will redeliver), but a
 * proper Redis/SQS implementation should land before this runs at
 * meaningful scale. The {@link JobQueue} interface keeps the seam
 * thin.</p>
 *
 * <p>Queue depth is unbounded — under runaway load this would push
 * the process toward OOM. A bounded queue with a {@code RejectedExecutionHandler}
 * is the right hardening once we have an SLO to defend.</p>
 */
@Slf4j
@Component
public class InMemoryJobQueue implements JobQueue {

    private final LinkedBlockingQueue<AutomationJob> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "automation-delay-scheduler");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void enqueue(AutomationJob job) {
        queue.offer(job);
    }

    @Override
    public void enqueueDelayed(AutomationJob job, Duration delay) {
        long ms = Math.max(0, delay.toMillis());
        scheduler.schedule(() -> queue.offer(job), ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<AutomationJob> poll(Duration timeout) throws InterruptedException {
        AutomationJob job = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return Optional.ofNullable(job);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        log.info("InMemoryJobQueue scheduler stopped. {} jobs were still queued.", queue.size());
    }
}
