package com.creatorengine.automation.cooldown;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class CooldownService {

    private static final Logger log = LoggerFactory.getLogger(CooldownService.class);

    private static final String USERS = "users";
    private static final String COOLDOWNS = "cooldowns";
    private static final String FIELD_FIRED_AT = "lastFiredAt";

    private final Firestore firestore;

    public CooldownService(Firestore firestore) {
        this.firestore = firestore;
    }

    public boolean canFire(String uid, Automation automation, WebhookEventDto event) {
        if (event == null || event.instagramUserId() == null) return true;

        boolean matchAllDm = isMatchAllDirectMessage(automation);
        int minutes = automation.getCooldownMinutes();

        // Nothing to limit: not a "reply to any message" DM, and no cooldown set.
        if (!matchAllDm && minutes <= 0) return true;

        String key = key(automation.getId(), event.instagramUserId());

        try {
            DocumentSnapshot snap = docRef(uid, key).get().get();

            // Never replied to this person for this automation yet -> allow.
            if (!snap.exists()) return true;

            // SAFETY: a DM/story automation that replies to ANY message reaches
            // everyone who contacts you. We reply to each person at most ONCE,
            // EVER -- a one-time auto-responder -- so it can never spam the
            // people you talk with regularly.
            if (matchAllDm) {
                log.debug("Once-per-person guard active uid={} automation={} sender={}",
                        uid, automation.getId(), event.instagramUserId());
                return false;
            }

            // Otherwise: normal time-based cooldown (keyword automations, comments).
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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.warn("Cooldown check failed uid={} key={}: {}", uid, key, e.getMessage());
            return true;
        }
    }

    public void recordFiring(String uid, Automation automation, WebhookEventDto event) {
        boolean matchAllDm = isMatchAllDirectMessage(automation);

        // Record a firing whenever we need to limit future ones:
        //  - a "reply to any message" DM (so once-per-person works), OR
        //  - a configured time-based cooldown.
        if (!matchAllDm && automation.getCooldownMinutes() <= 0) return;
        if (event == null || event.instagramUserId() == null) return;

        String key = key(automation.getId(), event.instagramUserId());

        try {
            docRef(uid, key).set(Map.of(
                    FIELD_FIRED_AT, Date.from(Instant.now())
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.warn("Cooldown record failed uid={} key={}: {}", uid, key, e.getMessage());
        }
    }

    /**
     * True when this automation replies to EVERY matching inbox event:
     * a DM, story-reply, or content-shared trigger whose condition is ANY (or unset).
     * These are limited to one reply per person so we never spam someone who
     * messages/shares repeatedly. Keyword DMs and comments are NOT match-all.
     */
    private static boolean isMatchAllDirectMessage(Automation automation) {
        if (automation == null || automation.getTrigger() == null) {
            return false;
        }
        String trigger = automation.getTrigger().name();
        boolean directInbox = "DM".equals(trigger)
                || "STORY_REPLY".equals(trigger)
                || "CONTENT_SHARED".equals(trigger)
                || "STORY_MENTION".equals(trigger);
        if (!directInbox) {
            return false;
        }

        Automation.Condition condition = automation.getCondition();
        if (condition == null || condition.getType() == null) {
            return true; // no condition configured = matches everything
        }
        return "ANY".equals(condition.getType().name());
    }

    private DocumentReference docRef(String uid, String key) {
        return firestore.collection(USERS).document(uid)
                .collection(COOLDOWNS).document(key);
    }

    private static String key(String automationId, String senderIgId) {
        return automationId + ":" + senderIgId;
    }
}