package com.creatorengine.automation.repository;

import com.creatorengine.automation.entity.ExecutionLog;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Append-only write path for automation execution logs.
 *
 * <p>Logs are mostly written, occasionally read (debugging UI). We
 * don't bother with batching — each log is one small write and
 * Firestore handles 500 writes/sec per collection comfortably.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExecutionLogRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "execution_logs";

    private final Firestore firestore;

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
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("ExecutionLogRepository.save failed", e);
            throw new RuntimeException("Firestore operation failed: save", e);
        }
    }

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
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("ExecutionLogRepository.listForUser failed", e);
            throw new RuntimeException("Firestore operation failed: listForUser", e);
        }
    }
}
