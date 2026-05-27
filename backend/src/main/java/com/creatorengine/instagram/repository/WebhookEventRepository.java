package com.creatorengine.instagram.repository;

import com.creatorengine.instagram.entity.WebhookEventRecord;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class WebhookEventRepository {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "instagram_events";
    private static final String ORPHAN_COLLECTION = "orphan_webhook_events";

    private final Firestore firestore;

    public WebhookEventRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference userEvents(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION);
    }

    private CollectionReference orphanEvents() {
        return firestore.collection(ORPHAN_COLLECTION);
    }

    public WebhookEventRecord saveForUser(String uid, WebhookEventRecord event) {
        return write(userEvents(uid), event);
    }

    public WebhookEventRecord saveOrphan(WebhookEventRecord event) {
        return write(orphanEvents(), event);
    }

    public List<WebhookEventRecord> listForUser(String uid, int limit) {
        try {
            return userEvents(uid)
                    .orderBy("receivedAt", Query.Direction.DESCENDING)
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(WebhookEventRecord.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForUser", e);
        }
    }

    private WebhookEventRecord write(CollectionReference col, WebhookEventRecord event) {
        try {
            if (event.getReceivedAt() == null) {
                event.setReceivedAt(Instant.now());
            }

            DocumentReference ref = event.getId() == null || event.getId().isBlank()
                    ? col.document()
                    : col.document(event.getId());

            event.setId(ref.getId());
            ref.set(event).get();

            return event;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("write", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        log.error("WebhookEventRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}