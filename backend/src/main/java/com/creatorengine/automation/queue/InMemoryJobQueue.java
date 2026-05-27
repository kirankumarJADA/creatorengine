package com.creatorengine.automation.queue;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryJobQueue implements JobQueue {

    private static final Logger log = LoggerFactory.getLogger(InMemoryJobQueue.class);

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