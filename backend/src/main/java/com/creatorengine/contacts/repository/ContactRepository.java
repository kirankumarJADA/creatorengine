package com.creatorengine.contacts.repository;

import com.creatorengine.contacts.entity.Contact;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class ContactRepository {

    private static final Logger log = LoggerFactory.getLogger(ContactRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String SUBCOLLECTION = "contacts";

    private final Firestore firestore;

    public ContactRepository(Firestore firestore) {
        this.firestore = firestore;
    }

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
        if (instagramUserId == null || instagramUserId.isBlank()) {
            return Optional.empty();
        }

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

    public Contact upsertByInstagramUserId(String uid, Contact incoming) {
        try {
            Instant now = Instant.now();
            Optional<Contact> existing = findByInstagramUserId(uid, incoming.getInstagramUserId());

            if (existing.isPresent()) {
                Contact merged = existing.get();

                if (incoming.getUsername() != null) {
                    merged.setUsername(incoming.getUsername());
                }

                if (incoming.getSource() != null) {
                    merged.setSource(incoming.getSource());
                }

                if (incoming.getLastMessage() != null) {
                    merged.setLastMessage(incoming.getLastMessage());
                }

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
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        log.error("ContactRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}