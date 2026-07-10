package com.creatorengine.automation.followup;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.MetaMessagingService;
import com.creatorengine.instagram.service.MetaMessagingService.AccessTokenContext;
import com.creatorengine.instagram.service.MetaMessagingService.ByUserId;
import com.creatorengine.instagram.service.MetaMessagingService.SendResult;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Polls "pending_follow_ups" every minute and sends any whose scheduledAt
 * has passed and are still PENDING (i.e. the contact never replied).
 *
 * IMPORTANT / NEEDS YOUR CONFIRMATION:
 * This reads the Instagram account from Firestore path
 * "instagram_accounts/{uid}" and the automation from
 * "users/{uid}/automations/{automationId}" - matching the structure seen
 * in earlier sessions (users/{uid}/automations, users/{uid}/failed_jobs,
 * etc). If your actual Instagram account storage path/collection name is
 * different, share your InstagramAccount lookup service/repository and
 * I'll swap this one line out - everything else stays the same.
 */
@Component
public class FollowUpSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpSchedulerService.class);
    private static final String COLLECTION = "pending_follow_ups";

    private final Firestore firestore;
    private final MetaMessagingService metaMessaging;

    public FollowUpSchedulerService(Firestore firestore, MetaMessagingService metaMessaging) {
        this.firestore = firestore;
        this.metaMessaging = metaMessaging;
    }

    /** Runs every 60 seconds. */
    @Scheduled(fixedDelay = 60_000)
    public void processDueFollowUps() {
        QuerySnapshot snapshot;
        try {
            snapshot = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "PENDING")
                    .whereLessThanOrEqualTo("scheduledAt", new Date())
                    .get()
                    .get();
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Follow-up poll query failed: {}", ex.getMessage());
            return;
        }

        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                sendOne(doc);
            } catch (Exception ex) {
                log.warn("Follow-up send failed for doc {}: {}", doc.getId(), ex.getMessage());
            }
        }
    }

    private void sendOne(QueryDocumentSnapshot doc) throws InterruptedException, ExecutionException {
        String uid = doc.getString("uid");
        String automationId = doc.getString("automationId");
        String instagramUserId = doc.getString("instagramUserId");
        String message = doc.getString("message");
        DocumentReference ref = doc.getReference();

        if (uid == null || instagramUserId == null || message == null) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        // Re-check the automation is still enabled and follow-up still wanted,
        // in case it was disabled/deleted after scheduling.
        DocumentSnapshot autoSnap = firestore.collection("users").document(uid)
                .collection("automations").document(automationId)
                .get().get();

        if (!autoSnap.exists()) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        Automation automation = autoSnap.toObject(Automation.class);
        if (automation == null || !automation.getEnabled() || !automation.getFollowUpEnabled()) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        // NEEDS CONFIRMATION: adjust this lookup if your Instagram account
        // storage differs from "instagram_accounts/{uid}".
        DocumentSnapshot acctSnap = firestore.collection("instagram_accounts")
                .document(uid).get().get();

        if (!acctSnap.exists()) {
            log.warn("No connected Instagram account for uid={} - cancelling follow-up.", uid);
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        InstagramAccount acct = acctSnap.toObject(InstagramAccount.class);
        if (acct == null) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        SendResult result = metaMessaging.sendText(new ByUserId(instagramUserId), message, tokenCtx);

        if (result.success()) {
            ref.update("status", "SENT", "sentAt", new Date(), "updatedAt", new Date());
            log.info("Follow-up sent for uid={} automation={} user={}", uid, automationId, instagramUserId);
        } else {
            log.warn("Follow-up send failed uid={} automation={} user={}: {}",
                    uid, automationId, instagramUserId, result.error());
            // Mark cancelled rather than retry-looping forever on a hard failure
            // (e.g. token revoked, user blocked the account).
            ref.update("status", "CANCELLED", "updatedAt", new Date());
        }
    }
}