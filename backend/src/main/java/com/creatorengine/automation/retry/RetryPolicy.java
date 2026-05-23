package com.creatorengine.automation.retry;

import com.creatorengine.automation.executor.ExecutionResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Decides whether a failed execution should be retried, and how long
 * to wait before the next attempt.
 *
 * <p>Three tries total. Backoff schedule comes from the spec:</p>
 * <pre>
 *   attempt 1 → first try
 *   attempt 2 → after 1s
 *   attempt 3 → after 3s
 *   beyond    → dead-letter (no retry after 10s wait, since the
 *               spec lists 10s as the wait BEFORE attempt 3 from
 *               attempt 2... see comment below for the reading)
 * </pre>
 *
 * <p>The spec lists "Backoff: 1 sec 3 sec 10 sec" for "Attempts: 1 2 3".
 * Reading that as three escalating waits — 1s before attempt 2, 3s
 * before attempt 3, and 10s as a final hard cap (or implied for an
 * attempt 4 that doesn't actually happen) is the friendly interpretation.
 * If the intent was different — say "1s wait before attempt 1" — that
 * would be unusual since attempt 1 has no waiting period by definition.
 * We use the escalating-waits reading and document it here.</p>
 */
@Component
public class RetryPolicy {

    /** Maximum total attempts including the first try. */
    public static final int MAX_ATTEMPTS = 3;

    /** Accessor used by the engine in log messages. */
    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }

    /** Index = attempt number BEFORE this wait. So waits[0] is before attempt 2. */
    private static final List<Duration> BACKOFFS = List.of(
            Duration.ofSeconds(1),   // before attempt 2
            Duration.ofSeconds(3),   // before attempt 3
            Duration.ofSeconds(10)   // would be before attempt 4 → never used, kept for clarity
    );

    /**
     * Should we retry given this failure result?
     *
     * <p>The retry decision is independent of the attempt counter; the
     * caller separately checks whether attempt limits remain.</p>
     */
    public boolean isRetryable(ExecutionResult result) {
        if (result == null || result.messageSent()) return false;

        // ExecutionResult.httpStatus() is the wire-level status when
        // MetaMessagingService.SendResult carried one. Null = non-HTTP
        // failure (e.g. config error, network exception); treat those
        // as retryable since they could be transient.
        Integer status = result.httpStatus();
        if (status == null || status == 0) {
            return true;
        }

        // 429: rate limited by Meta. Retry with backoff.
        if (status == 429) return true;
        // 5xx: server error. Retry.
        if (status >= 500 && status < 600) return true;

        // 401, 403: auth/permission. Token rot or scope problem — won't fix itself.
        // 4xx in general: client error (invalid recipient, malformed payload). Don't retry.
        return false;
    }

    /** True if we still have attempts remaining. */
    public boolean hasAttemptsLeft(int currentAttempt) {
        return currentAttempt < MAX_ATTEMPTS;
    }

    /**
     * How long to wait before {@code nextAttempt}. {@code nextAttempt}
     * is 1-based; passing 2 returns the wait before the second try.
     */
    public Duration backoffFor(int nextAttempt) {
        int idx = nextAttempt - 2; // attempt 2 → index 0
        if (idx < 0) return Duration.ZERO;
        if (idx >= BACKOFFS.size()) return BACKOFFS.get(BACKOFFS.size() - 1);
        return BACKOFFS.get(idx);
    }

    // ─── Helpers ─────────────────────────────────────────────
    // (No string-parsing helpers needed — ExecutionResult.httpStatus()
    // carries the wire status directly from MetaMessagingService.)
}
