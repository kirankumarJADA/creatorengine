package com.creatorengine.instagram.repository;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class InstagramAccountRepository {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "instagram_account";

    private final Firestore firestore;

    public InstagramAccountRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference docFor(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION)
                .document(InstagramAccount.DOC_ID);
    }

    public Optional<InstagramAccount> findByUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentSnapshot snap = docFor(uid).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(InstagramAccount.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByUid", e);
        }
    }

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

            if (docs.isEmpty()) {
                return Optional.empty();
            }

            QueryDocumentSnapshot doc = docs.get(0);
            String uid = doc.getReference().getParent().getParent().getId();
            InstagramAccount account = doc.toObject(InstagramAccount.class);

            return Optional.of(new OwnedAccount(uid, account));
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByInstagramUserId", e);
        }
    }

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
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        log.error("InstagramAccountRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    public record OwnedAccount(String uid, InstagramAccount account) {
    }
}