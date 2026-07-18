package com.creatorengine.automation.loopguard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit breaker for cross-account bot loops.
 *
 * Instagram scopes a sender's IGSID per RECEIVING account, so comparing an
 * incoming sender id directly against a creator's other connected accounts'
 * ids never matches — each of the creator's own pages sees the other under a
 * different scoped id. That made the old identity-based bot-loop guard in
 * AutomationEngine silently never trip, letting two of a creator's own pages
 * (e.g. one running a follow-gate automation, another running AI FAQ/
 * Autopilot) auto-reply to each other indefinitely. It's especially bad once
 * an AI system starts generating fresh wording each reply, since that also
 * defeats any content-based dedup.
 *
 * Instead of identity matching, this tracks a short rolling window of which
 * of the creator's connected accounts received an event recently. Real
 * customers only ever talk to ONE of the creator's accounts, so a burst of
 * events landing on 2+ distinct connected accounts for the same uid inside a
 * tight window is a reliable loop signature, with no false positives against
 * legitimate multi-turn conversations (which always stay on one account).
 */
@Component
public class CrossAccountLoopGuard {

    private static final Logger log = LoggerFactory.getLogger(CrossAccountLoopGuard.class);

    // If this many events land for the same uid, spanning 2+ distinct
    // connected accounts, inside the window below -> treat it as a loop.
    private static final int TRIP_THRESHOLD = 4;
    private static final long WINDOW_MILLIS = 20_000;

    private record Entry(String accountId, long atMillis) {}

    private final ConcurrentHashMap<String, Deque<Entry>> recent = new ConcurrentHashMap<>();

    /**
     * Records this event and returns true if the uid's recent activity looks
     * like a cross-account bot loop and this event should be dropped instead
     * of being handed to any automation/AI system.
     */
    public boolean recordAndCheckTripped(String uid, String igAccountId) {
        if (uid == null || igAccountId == null) return false;

        Deque<Entry> window = recent.computeIfAbsent(uid, k -> new ArrayDeque<>());
        long now = Instant.now().toEpochMilli();

        synchronized (window) {
            window.addLast(new Entry(igAccountId, now));
            while (!window.isEmpty() && now - window.peekFirst().atMillis() > WINDOW_MILLIS) {
                window.pollFirst();
            }

            if (window.size() < TRIP_THRESHOLD) {
                return false;
            }

            Set<String> distinctAccounts = new HashSet<>();
            for (Entry e : window) {
                distinctAccounts.add(e.accountId());
            }

            if (distinctAccounts.size() >= 2) {
                log.warn("Cross-account loop guard TRIPPED uid={} - {} events across {} of the creator's "
                                + "own connected accounts within {}s. Dropping event.",
                        uid, window.size(), distinctAccounts.size(), WINDOW_MILLIS / 1000);
                return true;
            }

            return false;
        }
    }
}
