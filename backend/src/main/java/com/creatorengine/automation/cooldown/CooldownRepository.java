package com.creatorengine.automation.cooldown;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class CooldownRepository {

    private static final Logger log = LoggerFactory.getLogger(CooldownRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "cooldowns";

    private final Firestore firestore;

    public CooldownRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference docFor(String uid, String automationId, String senderIgId) {
        String key = automationId + ":" + senderIgId;
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION)
                .document(key);
    }

    public Optional<CooldownEntry> find(String uid, String automationId, String senderIgId) {
        try {
            DocumentSnapshot snap = docFor(uid, automationId, senderIgId).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(CooldownEntry.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("CooldownRepository.find failed", e);
            return Optional.empty();
        }
    }

    public void save(String uid, CooldownEntry entry) {
        try {
            docFor(uid, entry.getAutomationId(), entry.getSenderInstagramUserId())
                    .set(entry).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("CooldownRepository.save failed: {}", e.getMessage());
        }
    }
}