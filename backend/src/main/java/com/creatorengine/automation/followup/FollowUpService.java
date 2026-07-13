package com.creatorengine.automation.followup;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
     * Now stores username and igAccountId for proper template rendering and
     * account lookup at send time.
     */
    public void scheduleOrReset(String uid, Automation automation, WebhookEventDto event) {
        if (uid == null || automation == null || automation.getId() == null || event == null) {
            return;
        }
        if (!automation.getFollowUpEnabled()) return;

        String instagramUserId = event.instagramUserId();
        if (instagramUserId == null || instagramUserId.isBlank()) return;

        String message = automation.getFollowUpMessage();
        if (message == null || message.isBlank()) {
            log.warn("Follow-up enabled for automation {} but message is empty - skipping.", automation.getId());
            return;
        }

        long delayMillis = automation.getFollowUpDelayUnit().toMillis(automation.getFollowUpDelayAmount());
        Date scheduledAt = new Date(System.currentTimeMillis() + delayMillis);

        String docId = docId(uid, automation.getId(), instagramUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("automationId", automation.getId());
        data.put("instagramUserId", instagramUserId);
        data.put("username", event.username() != null ? event.username() : "");
        data.put("igAccountId", event.receivingAccountId() != null ? event.receivingAccountId() : "");
        data.put("message", message);
        data.put("scheduledAt", scheduledAt);
        data.put("status", "PENDING");
        data.put("updatedAt", new Date());

        try {
            firestore.collection(COLLECTION).document(docId).set(data).get();
            log.info("Follow-up scheduled for uid={} automation={} user={} at={}",
                    uid, automation.getId(), instagramUserId, scheduledAt);
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Failed to schedule follow-up: {}", ex.getMessage());
        }
    }

    /**
     * Cancel any pending follow-up for this contact under this account.
     * Called when a new DM arrives from this user.
     */
    public void cancelPendingForUser(String uid, String instagramUserId) {
        if (uid == null || instagramUserId == null || instagramUserId.isBlank()) return;

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