package com.creatorengine.automation.queue;

import com.creatorengine.automation.engine.AutomationEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool of consumer threads pulling {@link AutomationJob}s from the
 * queue and handing them to {@link AutomationEngine#processJob}.
 *
 * <p>Talks to {@link JobQueue} via the interface, not the in-memory
 * implementation — when we swap to Redis/SQS the worker doesn't change.</p>
 *
 * <p>Worker count is configurable via {@code app.queue.workers} (default 2).
 * Workers are daemon threads so they don't block JVM shutdown; on
 * {@link PreDestroy} we set the running flag and interrupt them so
 * the {@code poll()} call returns immediately.</p>
 *
 * <p>Each worker's loop has a top-level try/catch that swallows
 * everything — a job that crashes a worker would otherwise silently
 * stop draining the queue. The engine itself is responsible for
 * retry/dead-letter logic; the worker is just the pump.</p>
 *
 * <p>Poll timeout (1s) is short enough that shutdowns feel snappy
 * but long enough that idle workers don't burn CPU.</p>
 */
@Slf4j
@Component
public class QueueWorker {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private final JobQueue queue;
    private final AutomationEngine engine;
    private final int workerCount;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final List<Thread> threads = new ArrayList<>();

    public QueueWorker(
            JobQueue queue,
            AutomationEngine engine,
            @Value("${app.queue.workers:2}") int workerCount
    ) {
        this.queue = queue;
        this.engine = engine;
        this.workerCount = Math.max(1, workerCount);
    }

    @PostConstruct
    void start() {
        running.set(true);
        for (int i = 0; i < workerCount; i++) {
            int id = i;
            Thread t = new Thread(() -> loop(id), "automation-worker-" + id);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        log.info("QueueWorker started with {} thread(s)", workerCount);
    }

    @PreDestroy
    void stop() {
        running.set(false);
        threads.forEach(Thread::interrupt);
        log.info("QueueWorker stopping. {} jobs processed during this lifetime.",
                processedCount.get());
    }

    private void loop(int id) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            AutomationJob job = null;
            try {
                var maybeJob = queue.poll(POLL_TIMEOUT);
                if (maybeJob.isEmpty()) continue;
                job = maybeJob.get();
                engine.processJob(job);
                processedCount.incrementAndGet();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                // Never let a bug take down a worker — log loudly and continue.
                log.error("Worker {} crashed processing job {}: {}",
                        id, job != null ? job.jobId() : "?", ex.getMessage(), ex);
            }
        }
        log.debug("Worker {} exiting cleanly.", id);
    }

    // ─── Stats (used by the health endpoint) ────────────────
    public WorkerStatus status() {
        long alive = threads.stream().filter(Thread::isAlive).count();
        return new WorkerStatus(workerCount, (int) alive, queue.size(), processedCount.get());
    }

    public record WorkerStatus(int configured, int alive, int queueDepth, long processedTotal) {}
}
