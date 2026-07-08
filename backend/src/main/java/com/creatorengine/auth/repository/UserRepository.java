package com.creatorengine.auth.repository;

import com.creatorengine.auth.entity.User;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private static final String COLLECTION = "users";

    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<User> findById(String uid) {
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentSnapshot snap = collection().document(uid).get().get();
            return snap.exists() ? Optional.ofNullable(snap.toObject(User.class)) : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById", e);
        }
    }
    public List<User> findAll() {
    try {
        return collection().get().get()
                .getDocuments().stream()
                .map(d -> d.toObject(User.class))
                .toList();
    } catch (InterruptedException | ExecutionException e) {
        throw wrap("findAll", e);
    }
}

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        try {
            QuerySnapshot result = collection()
                    .whereEqualTo("email", normalize(email))
                    .limit(1)
                    .get()
                    .get();

            if (result.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(result.getDocuments().get(0).toObject(User.class));
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findByEmail", e);
        }
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public User save(User user) {
        if (user.getUid() == null || user.getUid().isBlank()) {
            throw new IllegalArgumentException("User.uid must be set (use Firebase Auth UID).");
        }

        try {
            Instant now = Instant.now();

            if (user.getCreatedAt() == null) {
                user.setCreatedAt(now);
            }

            user.setUpdatedAt(now);
            collection().document(user.getUid()).set(user).get();

            return user;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public void deleteById(String uid) {
        try {
            collection().document(uid).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById", e);
        }
    }

    private CollectionReference collection() {
        return firestore.collection(COLLECTION);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        log.error("UserRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}