package com.creatorengine.instagram.repository;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Per-user Instagram account, stored at
 * {@code users/{uid}/instagram_account/profile}.
 *
 * <p>We use a fixed doc id ({@code "profile"}) because there's
 * always exactly one connected account per user — using a known id
 * means callers don't need to query before they can read or write.</p>
 *
 * <p>{@link #findByInstagramUserId(String)} does a collection-group
 * query across every user's subcollection so a webhook handler can
 * resolve "which CreatorEngine user owns this IG account" from a
 * single field lookup. That requires a collection-group index on
 * {@code instagram_account · instagramUserId} — create it in the
 * Firestore console before running in production.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InstagramAccountRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "instagram_account";

    private final Firestore firestore;

    private DocumentReference docFor(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION)
                .document(InstagramAccount.DOC_ID);
    }

    // ─── Reads ────────────────────────────────────────────────
    public Optional<InstagramAccount> findByUid(String uid) {
        if (uid == null || uid.isBlank()) return Optional.empty();
        try {
            DocumentSnapshot snap = docFor(uid).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(InstagramAccount.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByUid", e);
        }
    }

    /**
     * Collection-group lookup across every user's {@code instagram_account}
     * subcollection. Returns the (uid, account) pair for the first matching
     * doc, or empty if none.
     *
     * <p>Needs a collection-group index on field {@code instagramUserId}.</p>
     */
    public Optional<OwnedAccount> findByInstagramUserId(String instagramUserId) {
        if (instagramUserId == null || instagramUserId.isBlank()) return Optional.empty();
        try {
            var docs = firestore.collectionGroup(SUBCOLLECTION)
                    .whereEqualTo("instagramUserId", instagramUserId)
                    .limit(1)
                    .get().get()
                    .getDocuments();
            if (docs.isEmpty()) return Optional.empty();
            QueryDocumentSnapshot doc = docs.get(0);
            // Path is users/{uid}/instagram_account/profile — uid is the grandparent doc id.
            String uid = doc.getReference().getParent().getParent().getId();
            InstagramAccount account = doc.toObject(InstagramAccount.class);
            return Optional.of(new OwnedAccount(uid, account));
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByInstagramUserId", e);
        }
    }

    // ─── Writes ───────────────────────────────────────────────
    public InstagramAccount save(String uid, InstagramAccount account) {
        try {
            docFor(uid).set(account).get();
            return account;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public void deleteByUid(String uid) {
        try {
            docFor(uid).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteByUid", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("InstagramAccountRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    /** (uid, account) pair returned by collection-group lookups. */
    public record OwnedAccount(String uid, InstagramAccount account) {}
}
