package com.creatorengine.automation.cooldown;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Reads/writes cooldown markers at {@code users/{uid}/cooldowns/{key}}.
 *
 * <p>Doc ids use {@code automationId:senderInstagramUserId} — colons
 * are valid Firestore id characters and make the keys human-readable
 * in the console.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CooldownRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "cooldowns";

    private final Firestore firestore;

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
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("CooldownRepository.find failed", e);
            // On lookup failure: fail-open. Better to risk a double-fire than
            // to lock out an automation due to a transient Firestore hiccup.
            return Optional.empty();
        }
    }

    public void save(String uid, CooldownEntry entry) {
        try {
            docFor(uid, entry.getAutomationId(), entry.getSenderInstagramUserId())
                    .set(entry).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("CooldownRepository.save failed: {}", e.getMessage());
            // Non-fatal: the next event will just be allowed through.
        }
    }
}
