package com.creatorengine.automation.repository;

import com.creatorengine.automation.entity.ExecutionLog;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * MULTI-ACCOUNT: Execution logs are now scoped per Instagram account.
 *
 * New path:    users/{uid}/accounts/{igAccountId}/execution_logs/{id}
 * Legacy path: users/{uid}/execution_logs/{id}
 */
@Repository
public class ExecutionLogRepository {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLogRepository.class);

    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String SUBCOLLECTION = "execution_logs";

    private final Firestore firestore;

    public ExecutionLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    // ─── New per-account paths ────────────────────────────────────

    private CollectionReference collection(String uid, String igAccountId) {
        return firestore.collection(USERS).document(uid)
                .collection(ACCOUNTS).document(igAccountId)
                .collection(SUBCOLLECTION);
    }

    public ExecutionLog save(String uid, String igAccountId, ExecutionLog row) {
        try {
            DocumentReference ref = igAccountId != null && !igAccountId.isBlank()
                    ? collection(uid, igAccountId).document()
                    : legacyCollection(uid).document();
            row.setId(ref.getId());
            ref.set(row).get();
            return row;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public List<ExecutionLog> listForAccount(String uid, String igAccountId, int limit) {
        try {
            return collection(uid, igAccountId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(ExecutionLog.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForAccount", e);
        }
    }

    // ─── Legacy paths (backward compat) ──────────────────────────

    private CollectionReference legacyCollection(String uid) {
        return firestore.collection(USERS).document(uid).collection(SUBCOLLECTION);
    }

    /**
     * @deprecated Use save(uid, igAccountId, row) instead.
     */
    @Deprecated
    public ExecutionLog save(String uid, ExecutionLog row) {
        return save(uid, null, row);
    }

    public List<OwnedLog> listAllAcrossUsers(int limit) {
        try {
            return firestore.collectionGroup(SUBCOLLECTION)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> {
                        ExecutionLog l = d.toObject(ExecutionLog.class);
                        // Try to get uid from either new or legacy path
                        com.google.cloud.firestore.DocumentReference parent =
                                d.getReference().getParent().getParent();
                        String uid = null;
                        if (parent != null) {
                            String parentCollection = parent.getParent().getId();
                            if (ACCOUNTS.equals(parentCollection)) {
                                // New path: users/{uid}/accounts/{igId}/execution_logs
                                com.google.cloud.firestore.DocumentReference userDoc =
                                        parent.getParent().getParent();
                                uid = userDoc != null ? userDoc.getId() : null;
                            } else {
                                // Legacy path: users/{uid}/execution_logs
                                uid = parent.getId();
                            }
                        }
                        return new OwnedLog(uid, l);
                    })
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listAllAcrossUsers", e);
        }
    }

    /**
     * @deprecated Use listForAccount(uid, igAccountId, limit) instead.
     */
    @Deprecated
    public List<ExecutionLog> listForUser(String uid, int limit) {
        try {
            return legacyCollection(uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(ExecutionLog.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForUser(legacy)", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("ExecutionLogRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    public record OwnedLog(String uid, ExecutionLog log) {}
}