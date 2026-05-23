package com.creatorengine.automation.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-Instagram-account messages-per-minute throttle.
 *
 * <p>Sliding-window counter rather than a token bucket — keeps the
 * "messages per minute" mental model 1:1 with the configured limit
 * (a bucket with refill rate {@code limit/60} per second is the same
 * thing arithmetically but harder to reason about for ops).</p>
 *
 * <p>State is in-memory. A process restart resets all counters,
 * which is the conservative choice: post-restart we'd briefly
 * under-rate-limit, but Meta's own server-side limits still protect
 * us from a true runaway.</p>
 *
 * <p>This is a SOFT internal limit designed to keep accounts in good
 * standing — Meta's hard limits are tighter than this and live on
 * their side.</p>
 */
@Slf4j
@Service
public class RateLimitService {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int limitPerMinute;
    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    public RateLimitService(@Value("${app.rate-limit.messages-per-minute:30}") int limitPerMinute) {
        this.limitPerMinute = Math.max(1, limitPerMinute);
        log.info("RateLimitService: soft limit = {} msgs/min per IG account", this.limitPerMinute);
    }

    /**
     * @return true if a send is allowed under the current window. On
     *         {@code true} the call is recorded; the caller must NOT
     *         double-acquire.
     */
    public boolean tryAcquire(String igAccountId) {
        if (igAccountId == null || igAccountId.isBlank()) return true;

        Deque<Instant> dq = windows.computeIfAbsent(igAccountId, k -> new ArrayDeque<>());
        synchronized (dq) {
            Instant now = Instant.now();
            Instant cutoff = now.minus(WINDOW);

            // Drop entries that fell out of the window.
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

    /**
     * How long the caller should back off before retrying. Returns
     * {@link Duration#ZERO} when the window is empty.
     */
    public Duration suggestedBackoff(String igAccountId) {
        Deque<Instant> dq = windows.get(igAccountId);
        if (dq == null) return Duration.ZERO;
        synchronized (dq) {
            Instant oldest = dq.peekFirst();
            if (oldest == null) return Duration.ZERO;
            Instant freeAt = oldest.plus(WINDOW);
            long secs = Math.max(1, Duration.between(Instant.now(), freeAt).getSeconds());
            return Duration.ofSeconds(secs);
        }
    }

    /** Approximate current usage for the health endpoint. */
    public int currentLoad(String igAccountId) {
        Deque<Instant> dq = windows.get(igAccountId);
        if (dq == null) return 0;
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

    /** For the health endpoint. */
    public int trackedAccounts() {
        return windows.size();
    }
}
