package com.creatorengine.automation.repository;

import com.creatorengine.automation.entity.ExecutionLog;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class ExecutionLogRepository {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLogRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "execution_logs";

    private final Firestore firestore;

    public ExecutionLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference logsFor(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION);
    }

    public ExecutionLog save(String uid, ExecutionLog row) {
        try {
            DocumentReference ref = logsFor(uid).document();
            row.setId(ref.getId());
            ref.set(row).get();
            return row;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("ExecutionLogRepository.save failed", e);
            throw new RuntimeException("Firestore operation failed: save", e);
        }
    }

    public List<OwnedLog> listAllAcrossUsers(int limit) {
    try {
        return firestore.collectionGroup(SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(Math.max(1, Math.min(limit, 500)))
                .get().get()
                .getDocuments().stream()
                .map(d -> {
                    ExecutionLog log = d.toObject(ExecutionLog.class);
                    String uid = d.getReference().getParent().getParent() != null
                            ? d.getReference().getParent().getParent().getId()
                            : null;
                    return new OwnedLog(uid, log);
                })
                .toList();
    } catch (InterruptedException | ExecutionException e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("ExecutionLogRepository.listAllAcrossUsers failed", e);
        throw new RuntimeException("Firestore operation failed: listAllAcrossUsers", e);
    }
}

public record OwnedLog(String uid, ExecutionLog log) {}


    public List<ExecutionLog> listForUser(String uid, int limit) {
        try {
            return logsFor(uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(ExecutionLog.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("ExecutionLogRepository.listForUser failed", e);
            throw new RuntimeException("Firestore operation failed: listForUser", e);
        }
    }
}