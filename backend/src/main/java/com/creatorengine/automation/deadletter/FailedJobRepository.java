package com.creatorengine.automation.deadletter;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FailedJobRepository {

    private static final String USERS = "users";
    private static final String FAILED_JOBS = "failed_jobs";

    private final Firestore firestore;

    private CollectionReference collection(String uid) {
        return firestore.collection(USERS).document(uid).collection(FAILED_JOBS);
    }

    public FailedJob save(String uid, FailedJob job) {
        try {
            DocumentReference ref = collection(uid).document();
            job.setId(ref.getId());
            ref.set(job).get();
            return job;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Failed to persist dead-letter record uid={}: {}", uid, e.getMessage());
            // Don't throw — the job has already failed; we don't want to mask that
            // with a Firestore exception.
            return job;
        }
    }

    public List<FailedJob> listForUser(String uid, int limit) {
        try {
            return collection(uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(FailedJob.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("listForUser failed uid={}", uid, e);
            throw new RuntimeException("Firestore listForUser failed", e);
        }
    }

    /**
     * Find one failed-job row. Returns empty when the doc doesn't
     * exist — the controller maps that to a 404.
     */
    public java.util.Optional<FailedJob> findById(String uid, String id) {
        try {
            var snap = collection(uid).document(id).get().get();
            if (!snap.exists()) return java.util.Optional.empty();
            return java.util.Optional.ofNullable(snap.toObject(FailedJob.class));
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("findById failed uid={} id={}", uid, id, e);
            throw new RuntimeException("Firestore findById failed", e);
        }
    }

    /**
     * Hard-delete a failed-job row. Used both by the standalone Delete
     * action and by Retry (a successful retry removes the historical
     * row — if it fails again, the engine writes a new one).
     */
    public void delete(String uid, String id) {
        try {
            collection(uid).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("delete failed uid={} id={}", uid, id, e);
            throw new RuntimeException("Firestore delete failed", e);
        }
    }
}
