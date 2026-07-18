package com.creatorengine.autopilot.repository;

import com.creatorengine.autopilot.entity.AutopilotConversation;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class AutopilotConversationRepository {

    private static final Logger log = LoggerFactory.getLogger(AutopilotConversationRepository.class);
    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String CONVERSATIONS = "autopilotConversations";

    private final Firestore firestore;

    public AutopilotConversationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid, String igAccountId) {
        return firestore.collection(USERS).document(uid)
                .collection(ACCOUNTS).document(igAccountId)
                .collection(CONVERSATIONS);
    }

    public AutopilotConversation find(String uid, String igAccountId, String instagramUserId) {
        try {
            DocumentSnapshot snap = collection(uid, igAccountId).document(instagramUserId).get().get();
            if (!snap.exists()) return null;
            return snap.toObject(AutopilotConversation.class);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to read Autopilot conversation uid={} igAccountId={} ig={}: {}",
                    uid, igAccountId, instagramUserId, e.getMessage());
            return null;
        }
    }

    public void save(String uid, String igAccountId, AutopilotConversation conversation) {
        try {
            collection(uid, igAccountId).document(conversation.getInstagramUserId())
                    .set(conversation).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to save Autopilot conversation uid={} igAccountId={}: {}",
                    uid, igAccountId, e.getMessage());
        }
    }

    /** All conversations for an account — used to compute the stats panel. Fine at MVP scale. */
    public List<AutopilotConversation> listForAccount(String uid, String igAccountId) {
        try {
            return collection(uid, igAccountId).get().get().getDocuments().stream()
                    .map(d -> d.toObject(AutopilotConversation.class))
                    .filter(c -> c != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to list Autopilot conversations uid={} igAccountId={}: {}",
                    uid, igAccountId, e.getMessage());
            return List.of();
        }
    }
}
