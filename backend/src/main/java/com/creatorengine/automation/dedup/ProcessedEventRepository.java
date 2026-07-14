package com.creatorengine.automation.dedup;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class ProcessedEventRepository {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventRepository.class);

    private static final String COLLECTION = "processed_events";

    private final Firestore firestore;

    public ProcessedEventRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference doc(String dedupKey) {
        return firestore.collection(COLLECTION).document(dedupKey);
    }

    /**
     * Atomically claim this event for processing.
     * Uses Firestore's create() which fails with ALREADY_EXISTS if the
     * document exists — making the check-and-write a single atomic
     * operation. This prevents double-fire when Meta retries a webhook
     * (e.g. during a Render cold-start where the first delivery times out).
     *
     * @return true if this caller is the first to process this event (proceed),
     *         false if another request already claimed it (skip).
     */
    public boolean tryClaimAndSave(ProcessedEvent event) {
        try {
            doc(event.getId()).create(event).get();
            log.debug("Dedup claimed key={} uid={}", event.getId(), event.getUid());
            return true;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AlreadyExistsException) {
                log.info("Dedup: event already claimed, skipping. key={}", event.getId());
                return false;
            }
            log.warn("Dedup create failed for key={}: {}", event.getId(), e.getMessage());
            // On unexpected error allow processing (better to double-fire than drop)
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Dedup interrupted for key={}", event.getId());
            return true;
        }
    }

    /** @deprecated Use tryClaimAndSave for atomic dedup. Kept for legacy callers. */
    @Deprecated
    public boolean exists(String dedupKey) {
        try {
            return doc(dedupKey).get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Dedup existence check failed for key={}: {}", dedupKey, e.getMessage());
            return false;
        }
    }
}