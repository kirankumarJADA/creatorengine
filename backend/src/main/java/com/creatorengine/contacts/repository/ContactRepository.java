package com.creatorengine.contacts.repository;

import com.creatorengine.contacts.entity.Contact;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed repository for the per-user contacts subcollection.
 *
 * <p>The {@link #upsertByInstagramUserId} method is the workhorse —
 * the automation engine calls it on every successful execution to
 * either insert a new contact or refresh an existing one. Doing a
 * {@code whereEqualTo} lookup before each write costs one read per
 * fire, which is acceptable; if it becomes a hot path we'll switch
 * to deterministic doc ids derived from the IG user id.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ContactRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "contacts";

    private final Firestore firestore;

    private CollectionReference contactsFor(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SUBCOLLECTION);
    }

    public List<Contact> listForUser(String uid) {
        try {
            return contactsFor(uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Contact.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForUser", e);
        }
    }

    public Optional<Contact> findByInstagramUserId(String uid, String instagramUserId) {
        if (instagramUserId == null || instagramUserId.isBlank()) return Optional.empty();
        try {
            var docs = contactsFor(uid)
                    .whereEqualTo("instagramUserId", instagramUserId)
                    .limit(1)
                    .get().get()
                    .getDocuments();
            return docs.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(docs.get(0).toObject(Contact.class));
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByInstagramUserId", e);
        }
    }

    /**
     * Insert if absent, refresh if present. Always touches {@code updatedAt}
     * and increments {@code totalTriggers}; preserves {@code createdAt} on
     * existing rows.
     *
     * <p>Increment-on-upsert is the right invariant here: the engine
     * only calls this on a SUCCESSFUL automation execution, so
     * {@code totalTriggers} is "how many times this contact has caused
     * us to send something". A new contact starts at 1 because the
     * very call creating them is itself a trigger.</p>
     */
    public Contact upsertByInstagramUserId(String uid, Contact incoming) {
        try {
            Instant now = Instant.now();
            Optional<Contact> existing = findByInstagramUserId(uid, incoming.getInstagramUserId());

            if (existing.isPresent()) {
                Contact merged = existing.get();
                if (incoming.getUsername()    != null) merged.setUsername(incoming.getUsername());
                if (incoming.getSource()      != null) merged.setSource(incoming.getSource());
                if (incoming.getLastMessage() != null) merged.setLastMessage(incoming.getLastMessage());
                merged.setTotalTriggers(merged.getTotalTriggers() + 1);
                merged.setUpdatedAt(now);
                contactsFor(uid).document(merged.getId()).set(merged).get();
                return merged;
            }

            DocumentReference ref = contactsFor(uid).document();
            incoming.setId(ref.getId());
            incoming.setTotalTriggers(1L);
            incoming.setCreatedAt(now);
            incoming.setUpdatedAt(now);
            ref.set(incoming).get();
            return incoming;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("upsertByInstagramUserId", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("ContactRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}
