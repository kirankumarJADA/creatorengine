package com.creatorengine.automation.deadletter;

import com.creatorengine.automation.deadletter.FailedJob;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.queue.AutomationJob;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String FAILED_JOBS = "failed_jobs";

    private final Firestore firestore;

    public DeadLetterService(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid, String igAccountId) {
        return firestore.collection(USERS).document(uid)
                .collection(ACCOUNTS).document(igAccountId)
                .collection(FAILED_JOBS);
    }

    private CollectionReference legacyCollection(String uid) {
        return firestore.collection(USERS).document(uid).collection(FAILED_JOBS);
    }

    public void record(AutomationJob job, Automation automation, String reason) {
        if (job == null || job.uid() == null) return;

        String igAccountId = job.igAccountId();

        FailedJob failedJob = new FailedJob();
        failedJob.setAutomationId(job.automationId());
        failedJob.setAutomationName(automation != null ? automation.getName() : null);
        if (job.event() != null) {
            failedJob.setUsername(job.event().username());
        }
        failedJob.setReason(reason);
        failedJob.setAttempts(job.attempt());
        failedJob.setCreatedAt(Instant.now());

        try {
            DocumentReference ref = igAccountId != null && !igAccountId.isBlank()
                    ? collection(job.uid(), igAccountId).document()
                    : legacyCollection(job.uid()).document();
            failedJob.setId(ref.getId());
            ref.set(failedJob).get();
            log.info("Dead-lettered job {} uid={} igAccountId={} reason={}",
                    job.jobId(), job.uid(), igAccountId, reason);
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("DeadLetterService.record failed uid={}: {}", job.uid(), e.getMessage());
        }
    }

    public List<FailedJob> listForAccount(String uid, String igAccountId) {
        try {
            return collection(uid, igAccountId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(FailedJob.class))
                    .filter(j -> j != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForAccount", e);
        }
    }

    @Deprecated
    public List<FailedJob> listForUser(String uid) {
        try {
            return legacyCollection(uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(FailedJob.class))
                    .filter(j -> j != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForUser(legacy)", e);
        }
    }

    public Optional<FailedJob> findById(String uid, String igAccountId, String id) {
        try {
            DocumentSnapshot snap = igAccountId != null && !igAccountId.isBlank()
                    ? collection(uid, igAccountId).document(id).get().get()
                    : legacyCollection(uid).document(id).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(FailedJob.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById", e);
        }
    }

    public void deleteById(String uid, String igAccountId, String id) {
        try {
            DocumentReference ref = igAccountId != null && !igAccountId.isBlank()
                    ? collection(uid, igAccountId).document(id)
                    : legacyCollection(uid).document(id);
            ref.delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById", e);
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
                        FailedJob j = d.toObject(FailedJob.class);
                        com.google.cloud.firestore.DocumentReference parent =
                                d.getReference().getParent().getParent();
                        String uid = null;
                        if (parent != null) {
                            String parentColl = parent.getParent().getId();
                            if (ACCOUNTS.equals(parentColl)) {
                                com.google.cloud.firestore.DocumentReference userDoc =
                                        parent.getParent().getParent();
                                uid = userDoc != null ? userDoc.getId() : null;
                            } else {
                                uid = parent.getId();
                            }
                        }
                        return new OwnedFailedJob(uid, j);
                    })
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listAllAcrossUsers", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("DeadLetterService.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    public record OwnedFailedJob(String uid, FailedJob job) {}
}