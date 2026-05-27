package com.creatorengine.automation.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int limitPerMinute;
    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    public RateLimitService(@Value("${app.rate-limit.messages-per-minute:30}") int limitPerMinute) {
        this.limitPerMinute = Math.max(1, limitPerMinute);
        log.info("RateLimitService: soft limit = {} msgs/min per IG account", this.limitPerMinute);
    }

    public boolean tryAcquire(String igAccountId) {
        if (igAccountId == null || igAccountId.isBlank()) {
            return true;
        }

        Deque<Instant> dq = windows.computeIfAbsent(igAccountId, k -> new ArrayDeque<>());

        synchronized (dq) {
            Instant now = Instant.now();
            Instant cutoff = now.minus(WINDOW);

            while (!dq.isEmpty() && dq.peekFirst().isBefore(cutoff)) {
                dq.pollFirst();
            }

            if (dq.size() >= limitPerMinute) {
                log.debug("Rate limit hit ig={} count={}/{}",
                        igAccountId, dq.size(), limitPerMinute);
                return false;
            }

            dq.addLast(now);
            return true;
        }
    }

    public Duration suggestedBackoff(String igAccountId) {
        Deque<Instant> dq = windows.get(igAccountId);
        if (dq == null) {
            return Duration.ZERO;
        }

        synchronized (dq) {
            Instant oldest = dq.peekFirst();
            if (oldest == null) {
                return Duration.ZERO;
            }

            Instant freeAt = oldest.plus(WINDOW);
            long secs = Math.max(1, Duration.between(Instant.now(), freeAt).getSeconds());
            return Duration.ofSeconds(secs);
        }
    }

    public int currentLoad(String igAccountId) {
        Deque<Instant> dq = windows.get(igAccountId);
        if (dq == null) {
            return 0;
        }

        synchronized (dq) {
            Instant cutoff = Instant.now().minus(WINDOW);
            while (!dq.isEmpty() && dq.peekFirst().isBefore(cutoff)) {
                dq.pollFirst();
            }
            return dq.size();
        }
    }

    public int limitPerMinute() {
        return limitPerMinute;
    }

    public int trackedAccounts() {
        return windows.size();
    }
}