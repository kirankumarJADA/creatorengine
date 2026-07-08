package com.creatorengine.automation.deadletter;

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
public class FailedJobRepository {

    private static final Logger log = LoggerFactory.getLogger(FailedJobRepository.class);

    private static final String USERS = "users";
    private static final String FAILED_JOBS = "failed_jobs";

    private final Firestore firestore;

    public FailedJobRepository(Firestore firestore) {
        this.firestore = firestore;
    }

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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to persist dead-letter record uid={}: {}", uid, e.getMessage());
            return job;
        }
    }

    public List<OwnedFailedJob> listAllAcrossUsers(int limit) {
    try {
        return firestore.collectionGroup(FAILED_JOBS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(Math.max(1, Math.min(limit, 500)))
                .get().get()
                .getDocuments().stream()
                .map(d -> {
                    FailedJob job = d.toObject(FailedJob.class);
                    String uid = d.getReference().getParent().getParent() != null
                            ? d.getReference().getParent().getParent().getId()
                            : null;
                    return new OwnedFailedJob(uid, job);
                })
                .toList();
    } catch (InterruptedException | ExecutionException e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("FailedJobRepository.listAllAcrossUsers failed", e);
        throw new RuntimeException("Firestore listAllAcrossUsers failed", e);
    }
}

public record OwnedFailedJob(String uid, FailedJob job) {}

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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("listForUser failed uid={}", uid, e);
            throw new RuntimeException("Firestore listForUser failed", e);
        }
    }

    public java.util.Optional<FailedJob> findById(String uid, String id) {
        try {
            var snap = collection(uid).document(id).get().get();
            if (!snap.exists()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.ofNullable(snap.toObject(FailedJob.class));
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("findById failed uid={} id={}", uid, id, e);
            throw new RuntimeException("Firestore findById failed", e);
        }
    }

    public void delete(String uid, String id) {
        try {
            collection(uid).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("delete failed uid={} id={}", uid, id, e);
            throw new RuntimeException("Firestore delete failed", e);
        }
    }
}