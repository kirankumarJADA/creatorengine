package com.creatorengine.automation.cooldown;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Per-(automation, sender) anti-spam cooldown.
 *
 * <p>Stored at {@code users/{uid}/cooldowns/{automationId}:{senderIgId}}.
 * Doc id is composite so the same sender can independently cool down
 * across different automations.</p>
 *
 * <p>Persistent (not in-memory) because a process restart shouldn't
 * accidentally re-open the floodgates. Reads are one round trip per
 * dispatched job — acceptable for the per-user automation counts we
 * expect (typically &lt; 10).</p>
 *
 * <p>The check is "have we fired in the last N minutes?". On positive
 * answer, the dispatch is dropped silently — no log, no dead-letter.
 * Silently dropping is intentional: the spec's example ("same user
 * comments 'link' 20 times") explicitly wants quiet suppression.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CooldownService {

    private static final String USERS = "users";
    private static final String COOLDOWNS = "cooldowns";
    private static final String FIELD_FIRED_AT = "lastFiredAt";

    private final Firestore firestore;

    /**
     * @return true when this (automation, sender) pair is free to fire.
     */
    public boolean canFire(String uid, Automation automation, WebhookEventDto event) {
        int minutes = automation.getCooldownMinutes();
        if (minutes <= 0) return true;
        if (event == null || event.instagramUserId() == null) return true;

        String key = key(automation.getId(), event.instagramUserId());
        try {
            DocumentSnapshot snap = docRef(uid, key).get().get();
            if (!snap.exists()) return true;

            Date lastFired = snap.getDate(FIELD_FIRED_AT);
            if (lastFired == null) return true;

            Instant cutoff = Instant.now().minusSeconds(minutes * 60L);
            boolean cool = lastFired.toInstant().isBefore(cutoff);
            if (!cool) {
                log.debug("Cooldown active uid={} automation={} sender={} ({}m)",
                        uid, automation.getId(), event.instagramUserId(), minutes);
            }
            return cool;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Cooldown check failed uid={} key={}: {}", uid, key, e.getMessage());
            // Fail open — better to occasionally over-fire than to lock the user out.
            return true;
        }
    }

    /** Stamp the cooldown clock. Call this AFTER a successful execution. */
    public void recordFiring(String uid, Automation automation, WebhookEventDto event) {
        if (automation.getCooldownMinutes() <= 0) return;
        if (event == null || event.instagramUserId() == null) return;

        String key = key(automation.getId(), event.instagramUserId());
        try {
            docRef(uid, key).set(Map.of(
                    FIELD_FIRED_AT, Date.from(Instant.now())
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Cooldown record failed uid={} key={}: {}", uid, key, e.getMessage());
        }
    }

    private DocumentReference docRef(String uid, String key) {
        return firestore.collection(USERS).document(uid)
                .collection(COOLDOWNS).document(key);
    }

    private static String key(String automationId, String senderIgId) {
        // Composite doc id: automationId : senderIgId. Colon is valid in Firestore doc ids.
        return automationId + ":" + senderIgId;
    }
}
