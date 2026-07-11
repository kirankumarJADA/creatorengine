package com.creatorengine.instagram.repository;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * MULTI-ACCOUNT: Accounts are now stored as individual documents in a
 * subcollection, keyed by the Instagram user ID.
 *
 * Path: users/{uid}/instagram_accounts/{instagramUserId}
 *
 * BACKWARD COMPAT: The old single-doc path was:
 *   users/{uid}/instagram_account/primary
 * On first multi-account use, existing accounts are migrated automatically
 * by InstagramAccountService.migrateIfNeeded().
 */
@Repository
public class InstagramAccountRepository {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "instagram_accounts";

    // Old single-account path (for migration only)
    private static final String LEGACY_SUBCOLLECTION = "instagram_account";
    private static final String LEGACY_DOC_ID = "primary";

    private final Firestore firestore;

    public InstagramAccountRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference docFor(String uid, String instagramUserId) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION)
                .document(instagramUserId);
    }

    /**
     * Find a specific account by uid + instagramUserId.
     */
    public Optional<InstagramAccount> findByUidAndIgId(String uid, String instagramUserId) {
        if (uid == null || uid.isBlank() || instagramUserId == null || instagramUserId.isBlank()) {
            return Optional.empty();
        }
        try {
            DocumentSnapshot snap = docFor(uid, instagramUserId).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(InstagramAccount.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByUidAndIgId", e);
        }
    }

    /**
     * Find the first connected account for a user (for backward compat).
     * Returns the first account found — use findAllByUid for multi-account.
     */
    public Optional<InstagramAccount> findByUid(String uid) {
        return findAllByUid(uid).stream().findFirst();
    }

    /**
     * Find ALL connected accounts for a user.
     */
    public List<InstagramAccount> findAllByUid(String uid) {
        if (uid == null || uid.isBlank()) return List.of();
        try {
            List<QueryDocumentSnapshot> docs = firestore
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(SUBCOLLECTION)
                    .get().get()
                    .getDocuments();

            return docs.stream()
                    .map(d -> d.toObject(InstagramAccount.class))
                    .filter(a -> a != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllByUid", e);
        }
    }

    /**
     * Count how many accounts a user has connected.
     */
    public int countByUid(String uid) {
        return findAllByUid(uid).size();
    }

    /**
     * Find any account across all users by its Instagram user ID.
     * Used by webhook dispatch to find which user owns a given IG account.
     */
    public Optional<OwnedAccount> findByInstagramUserId(String instagramUserId) {
        if (instagramUserId == null || instagramUserId.isBlank()) {
            return Optional.empty();
        }
        try {
            var docs = firestore.collectionGroup(SUBCOLLECTION)
                    .whereEqualTo("instagramUserId", instagramUserId)
                    .limit(1)
                    .get().get()
                    .getDocuments();

            if (docs.isEmpty()) return Optional.empty();

            QueryDocumentSnapshot doc = docs.get(0);
            String uid = doc.getReference().getParent().getParent().getId();
            InstagramAccount account = doc.toObject(InstagramAccount.class);
            return Optional.of(new OwnedAccount(uid, account));
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByInstagramUserId", e);
        }
    }

    /**
     * All connected accounts across all users (for token-refresh sweep).
     */
    public List<OwnedAccount> findAll() {
        try {
            var docs = firestore.collectionGroup(SUBCOLLECTION)
                    .get().get()
                    .getDocuments();

            List<OwnedAccount> result = new ArrayList<>(docs.size());
            for (QueryDocumentSnapshot doc : docs) {
                String uid = doc.getReference().getParent().getParent().getId();
                InstagramAccount account = doc.toObject(InstagramAccount.class);
                result.add(new OwnedAccount(uid, account));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAll", e);
        }
    }

    /**
     * Save/update an account. Uses instagramUserId as the document key.
     */
    public InstagramAccount save(String uid, InstagramAccount account) {
        if (account.getInstagramUserId() == null || account.getInstagramUserId().isBlank()) {
            throw new IllegalArgumentException("InstagramAccount must have instagramUserId set before saving.");
        }
        try {
            docFor(uid, account.getInstagramUserId()).set(account).get();
            return account;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    /**
     * Delete a specific account by uid + instagramUserId.
     */
    public void deleteByUidAndIgId(String uid, String instagramUserId) {
        try {
            docFor(uid, instagramUserId).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteByUidAndIgId", e);
        }
    }

    /**
     * Legacy: delete by uid only (deletes the first account found).
     * Kept for backward compat — prefer deleteByUidAndIgId.
     */
    public void deleteByUid(String uid) {
        findAllByUid(uid).forEach(a -> deleteByUidAndIgId(uid, a.getInstagramUserId()));
    }

    /**
     * Read the legacy single-account doc (migration only).
     */
    public Optional<InstagramAccount> findLegacy(String uid) {
        try {
            DocumentSnapshot snap = firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(LEGACY_SUBCOLLECTION)
                    .document(LEGACY_DOC_ID)
                    .get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(InstagramAccount.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("findLegacy failed for uid={}: {}", uid, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete the legacy single-account doc after migration.
     */
    public void deleteLegacy(String uid) {
        try {
            firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(LEGACY_SUBCOLLECTION)
                    .document(LEGACY_DOC_ID)
                    .delete().get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("deleteLegacy failed for uid={}: {}", uid, e.getMessage());
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("InstagramAccountRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    public record OwnedAccount(String uid, InstagramAccount account) {}
}