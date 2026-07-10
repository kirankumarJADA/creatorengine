package com.creatorengine.automation.followup;

import com.creatorengine.automation.entity.Automation;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Manages the single "no-reply follow-up" per automation per contact.
 *
 * Storage: Firestore collection "pending_follow_ups". Document ID is
 * deterministic: {uid}_{automationId}_{instagramUserId} - this makes
 * scheduling idempotent. If the automation's chain sends multiple
 * messages, each successful send simply overwrites the same document,
 * resetting the timer to "now + delay" from the LAST message sent -
 * exactly matching the spec ("timer starts immediately after the
 * previous message is sent").
 *
 * Statuses: PENDING -> SENT | CANCELLED
 */
@Service
public class FollowUpService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);
    private static final String COLLECTION = "pending_follow_ups";

    private final Firestore firestore;

    public FollowUpService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Schedule (or reschedule) the single follow-up for this automation + contact.
     * Called after a successful send when automation.followUpEnabled is true.
     */
    public void scheduleOrReset(String uid, Automation automation, String instagramUserId) {
        if (uid == null || automation == null || automation.getId() == null
                || instagramUserId == null || instagramUserId.isBlank()) {
            return;
        }
        if (!automation.getFollowUpEnabled()) {
            return;
        }

        String message = automation.getFollowUpMessage();
        if (message == null || message.isBlank()) {
            log.warn("Follow-up enabled for automation {} but message is empty - skipping schedule.",
                    automation.getId());
            return;
        }

        long delayMillis = automation.getFollowUpDelayUnit().toMillis(automation.getFollowUpDelayAmount());
        Date scheduledAt = new Date(System.currentTimeMillis() + delayMillis);

        String docId = docId(uid, automation.getId(), instagramUserId);

        Map<String, Object> data = Map.of(
                "uid", uid,
                "automationId", automation.getId(),
                "instagramUserId", instagramUserId,
                "message", message,
                "scheduledAt", scheduledAt,
                "status", "PENDING",
                "updatedAt", new Date()
        );

        try {
            firestore.collection(COLLECTION).document(docId).set(data).get();
            log.info("Follow-up scheduled/reset for uid={} automation={} user={} at={}",
                    uid, automation.getId(), instagramUserId, scheduledAt);
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Failed to schedule follow-up: {}", ex.getMessage());
        }
    }

    /**
     * Cancel ANY pending follow-up(s) for this contact under this account,
     * regardless of which automation scheduled them. Called the moment a
     * new DM arrives from this user - a reply is a reply, it cancels
     * whatever follow-up(s) were waiting on them.
     */
    public void cancelPendingForUser(String uid, String instagramUserId) {
        if (uid == null || instagramUserId == null || instagramUserId.isBlank()) {
            return;
        }

        try {
            QuerySnapshot snapshot = firestore.collection(COLLECTION)
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("instagramUserId", instagramUserId)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .get();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                DocumentReference ref = doc.getReference();
                ref.update("status", "CANCELLED", "updatedAt", new Date());
                log.info("Cancelled pending follow-up {} - contact replied.", ref.getId());
            }
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Failed to cancel follow-ups for uid={} user={}: {}", uid, instagramUserId, ex.getMessage());
        }
    }

    private static String docId(String uid, String automationId, String instagramUserId) {
        return uid + "_" + automationId + "_" + instagramUserId;
    }
}