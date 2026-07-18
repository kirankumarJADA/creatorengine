package com.creatorengine.automation.followgate;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Tracks, per (uid, automationId, instagramUserId), whether a contact has
 * already completed a given automation's follow gate. Without this, every
 * new matching message re-asks "make sure you're following me" forever,
 * even for people who already followed and tapped through once.
 */
@Repository
public class FollowGateCompletionRepository {

    private static final Logger log = LoggerFactory.getLogger(FollowGateCompletionRepository.class);
    private static final String COLLECTION = "follow_gate_completions";

    private final Firestore firestore;

    public FollowGateCompletionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private String docId(String uid, String automationId, String instagramUserId) {
        return uid + "_" + automationId + "_" + instagramUserId;
    }

    public boolean hasCompleted(String uid, String automationId, String instagramUserId) {
        if (uid == null || automationId == null || instagramUserId == null) return false;
        try {
            DocumentReference ref = firestore.collection(COLLECTION)
                    .document(docId(uid, automationId, instagramUserId));
            return ref.get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to check follow-gate completion uid={} automationId={} ig={}: {}",
                    uid, automationId, instagramUserId, e.getMessage());
            return false; // fail open — worst case is one extra ask, never a hard block
        }
    }

    public void markCompleted(String uid, String automationId, String instagramUserId) {
        if (uid == null || automationId == null || instagramUserId == null) return;
        try {
            DocumentReference ref = firestore.collection(COLLECTION)
                    .document(docId(uid, automationId, instagramUserId));
            ref.set(Map.of(
                    "uid", uid,
                    "automationId", automationId,
                    "instagramUserId", instagramUserId,
                    "completedAt", new Date()
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to record follow-gate completion uid={} automationId={} ig={}: {}",
                    uid, automationId, instagramUserId, e.getMessage());
        }
    }
}
