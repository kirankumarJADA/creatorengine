package com.creatorengine.contacts.repository;

import com.creatorengine.contacts.entity.Contact;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class ContactRepository {

    private static final Logger log = LoggerFactory.getLogger(ContactRepository.class);

    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String CONTACTS = "contacts";

    private final Firestore firestore;

    public ContactRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    // ─── New per-account paths ────────────────────────────────────

    private CollectionReference collection(String uid, String igAccountId) {
        return firestore.collection(USERS).document(uid)
                .collection(ACCOUNTS).document(igAccountId)
                .collection(CONTACTS);
    }

    public List<Contact> listForAccount(String uid, String igAccountId) {
        try {
            return collection(uid, igAccountId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Contact.class))
                    .filter(c -> c != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForAccount", e);
        }
    }

    public Contact upsertByInstagramUserId(String uid, String igAccountId, Contact contact) {
        if (contact.getInstagramUserId() == null) {
            throw new IllegalArgumentException("Contact must have instagramUserId");
        }

        DocumentReference ref = collection(uid, igAccountId).document(contact.getInstagramUserId());

        try {
            DocumentSnapshot snap = ref.get().get();
            Contact existing = snap.exists() ? snap.toObject(Contact.class) : null;

            if (existing != null) {
                if (contact.getUsername() != null) existing.setUsername(contact.getUsername());
                if (contact.getSource() != null) existing.setSource(contact.getSource());
                if (contact.getLastMessage() != null) existing.setLastMessage(contact.getLastMessage());
                existing.setUpdatedAt(Instant.now());
                existing.setTotalTriggers(existing.getTotalTriggers() + 1L);
                ref.set(existing).get();
                return existing;
            } else {
                contact.setCreatedAt(Instant.now());
                contact.setUpdatedAt(Instant.now());
                contact.setTotalTriggers(1L);
                ref.set(contact).get();
                return contact;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("upsertByInstagramUserId(per-account)", e);
        }
    }

    // ─── Legacy single-account path ───────────────────────────────

    private CollectionReference legacyCollection(String uid) {
        return firestore.collection(USERS).document(uid).collection(CONTACTS);
    }

    @Deprecated
    public List<Contact> listForUser(String uid) {
        try {
            return legacyCollection(uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Contact.class))
                    .filter(c -> c != null)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("listForUser(legacy)", e);
        }
    }

    @Deprecated
    public Contact upsertByInstagramUserId(String uid, Contact contact) {
        if (contact.getInstagramUserId() == null) {
            throw new IllegalArgumentException("Contact must have instagramUserId");
        }

        DocumentReference ref = legacyCollection(uid).document(contact.getInstagramUserId());

        try {
            DocumentSnapshot snap = ref.get().get();
            Contact existing = snap.exists() ? snap.toObject(Contact.class) : null;

            if (existing != null) {
                if (contact.getUsername() != null) existing.setUsername(contact.getUsername());
                if (contact.getSource() != null) existing.setSource(contact.getSource());
                if (contact.getLastMessage() != null) existing.setLastMessage(contact.getLastMessage());
                existing.setUpdatedAt(Instant.now());
                existing.setTotalTriggers(existing.getTotalTriggers() + 1L);
                ref.set(existing).get();
                return existing;
            } else {
                contact.setCreatedAt(Instant.now());
                contact.setUpdatedAt(Instant.now());
                contact.setTotalTriggers(1L);
                ref.set(contact).get();
                return contact;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("upsertByInstagramUserId(legacy)", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("ContactRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}