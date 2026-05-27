package com.creatorengine.automation.dedup;

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

    public boolean exists(String dedupKey) {
        try {
            return doc(dedupKey).get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Dedup existence check failed for key={}: {}", dedupKey, e.getMessage());
            return false;
        }
    }

    public void save(ProcessedEvent event) {
        try {
            doc(event.getId()).set(event).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to record processed event {}: {}", event.getId(), e.getMessage());
        }
    }
}