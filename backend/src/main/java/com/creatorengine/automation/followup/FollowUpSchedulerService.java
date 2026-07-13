package com.creatorengine.automation.followup;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class FollowUpSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpSchedulerService.class);
    private static final String COLLECTION = "pending_follow_ups";

    private final Firestore firestore;
    private final MetaMessagingService metaMessaging;
    private final InstagramAccountService instagramAccountService;

    public FollowUpSchedulerService(
            Firestore firestore,
            MetaMessagingService metaMessaging,
            InstagramAccountService instagramAccountService
    ) {
        this.firestore = firestore;
        this.metaMessaging = metaMessaging;
        this.instagramAccountService = instagramAccountService;
    }

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
        String username = doc.getString("username"); // stored at schedule time if available
        DocumentReference ref = doc.getReference();

        if (uid == null || instagramUserId == null || message == null) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        // Re-check automation is still enabled
        // Try new per-account path first, then fall back to legacy
        DocumentSnapshot autoSnap = null;
        String igAccountId = doc.getString("igAccountId");

        if (igAccountId != null && !igAccountId.isBlank()) {
            autoSnap = firestore.collection("users").document(uid)
                    .collection("accounts").document(igAccountId)
                    .collection("automations").document(automationId)
                    .get().get();
        }

        if (autoSnap == null || !autoSnap.exists()) {
            // Try legacy path
            autoSnap = firestore.collection("users").document(uid)
                    .collection("automations").document(automationId)
                    .get().get();
        }

        if (!autoSnap.exists()) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        Automation automation = autoSnap.toObject(Automation.class);
        if (automation == null || !automation.getEnabled() || !automation.getFollowUpEnabled()) {
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        // Look up account — try by specific igAccountId first, fall back to find(uid)
        Optional<InstagramAccount> acctOpt = igAccountId != null
                ? instagramAccountService.findByIgId(uid, igAccountId)
                : instagramAccountService.find(uid);

        if (acctOpt.isEmpty()) {
            log.warn("No connected Instagram account for uid={} - cancelling follow-up.", uid);
            ref.update("status", "CANCELLED", "updatedAt", new Date());
            return;
        }

        InstagramAccount acct = acctOpt.get();

        // Render {{username}} — use stored username or generic fallback
        String displayName = (username != null && !username.isBlank()) ? username : "there";
        String renderedMessage = message.replace("{{username}}", displayName);

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        SendResult result = metaMessaging.sendText(new ByUserId(instagramUserId), renderedMessage, tokenCtx);

        if (result.success()) {
            ref.update("status", "SENT", "sentAt", new Date(), "updatedAt", new Date());
            log.info("Follow-up sent for uid={} automation={} user={}", uid, automationId, instagramUserId);
        } else {
            log.warn("Follow-up send failed uid={} automation={} user={}: {}",
                    uid, automationId, instagramUserId, result.error());
            ref.update("status", "CANCELLED", "updatedAt", new Date());
        }
    }
}