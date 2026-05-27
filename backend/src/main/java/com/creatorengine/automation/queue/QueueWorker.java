package com.creatorengine.automation.queue;

import com.creatorengine.automation.engine.AutomationEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class QueueWorker {

    private static final Logger log = LoggerFactory.getLogger(QueueWorker.class);

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
                if (maybeJob.isEmpty()) {
                    continue;
                }

                job = maybeJob.get();
                engine.processJob(job);
                processedCount.incrementAndGet();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                log.error("Worker {} crashed processing job {}: {}",
                        id, job != null ? job.jobId() : "?", ex.getMessage(), ex);
            }
        }

        log.debug("Worker {} exiting cleanly.", id);
    }

    public WorkerStatus status() {
        long alive = threads.stream().filter(Thread::isAlive).count();
        return new WorkerStatus(workerCount, (int) alive, queue.size(), processedCount.get());
    }

    public record WorkerStatus(int configured, int alive, int queueDepth, long processedTotal) {
    }
}