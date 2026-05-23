package com.creatorengine.automation.dedup;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Firestore access for the top-level {@code processed_events}
 * collection. Doc id is the dedup key, so we never need indexes.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProcessedEventRepository {

    private static final String COLLECTION = "processed_events";

    private final Firestore firestore;

    private DocumentReference doc(String dedupKey) {
        return firestore.collection(COLLECTION).document(dedupKey);
    }

    public boolean exists(String dedupKey) {
        try {
            return doc(dedupKey).get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Dedup existence check failed for key={}: {}", dedupKey, e.getMessage());
            // Fail open — better to occasionally double-process than to
            // drop events because Firestore had a hiccup.
            return false;
        }
    }

    public void save(ProcessedEvent event) {
        try {
            doc(event.getId()).set(event).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Failed to record processed event {}: {}", event.getId(), e.getMessage());
            // Fail open — see exists().
        }
    }
}
