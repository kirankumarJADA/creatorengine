package com.creatorengine.automation.repository;

import com.creatorengine.automation.entity.Automation;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore repository for the per-user automations subcollection at
 * {@code users/{uid}/automations}.
 *
 * <p>Each public method takes the owner's uid explicitly — there's no
 * implicit "current user" here. The service layer pulls the uid from
 * {@link com.creatorengine.security.SecurityUtils} and passes it in,
 * which keeps the repository testable and side-effect-free.</p>
 *
 * <p>Why a hand-rolled repo rather than Spring Data Firestore? The
 * community Spring Data Firestore module lags the Admin SDK and
 * surfaces subcollections awkwardly. A thin manual layer is easier
 * to reason about and gives us full control over indexing.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AutomationRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String AUTOMATIONS_SUBCOLLECTION = "automations";

    private final Firestore firestore;

    // ─── Path helpers ────────────────────────────────────────
    private CollectionReference collection(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(AUTOMATIONS_SUBCOLLECTION);
    }

    private DocumentReference document(String uid, String id) {
        return collection(uid).document(id);
    }

    // ─── Reads ───────────────────────────────────────────────
    public List<Automation> findAllByOwner(String uid) {
        try {
            return collection(uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Automation.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllByOwner", e);
        }
    }

    public Optional<Automation> findById(String uid, String id) {
        if (uid == null || id == null || id.isBlank()) return Optional.empty();
        try {
            DocumentSnapshot snap = document(uid, id).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(Automation.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById", e);
        }
    }

    // ─── Writes ──────────────────────────────────────────────
    public Automation save(String uid, Automation automation) {
        try {
            Instant now = Instant.now();
            if (automation.getId() == null || automation.getId().isBlank()) {
                DocumentReference ref = collection(uid).document();
                automation.setId(ref.getId());
                automation.setCreatedAt(now);
                automation.setUpdatedAt(now);
                ref.set(automation).get();
            } else {
                automation.setUpdatedAt(now);
                document(uid, automation.getId()).set(automation).get();
            }
            return automation;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public void deleteById(String uid, String id) {
        try {
            document(uid, id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById", e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────
    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("AutomationRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}
