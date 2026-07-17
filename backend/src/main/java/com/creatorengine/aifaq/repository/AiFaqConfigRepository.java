package com.creatorengine.aifaq.repository;

import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Firestore path: users/{uid}/accounts/{igAccountId}/aiFaq/config
 * Single doc per Instagram account, mirrors the automations path convention.
 */
@Repository
public class AiFaqConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(AiFaqConfigRepository.class);

    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String AI_FAQ = "aiFaq";
    private static final String CONFIG_DOC = "config";

    private final Firestore firestore;

    public AiFaqConfigRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference document(String uid, String igAccountId) {
        return firestore.collection(USERS)
                .document(uid)
                .collection(ACCOUNTS)
                .document(igAccountId)
                .collection(AI_FAQ)
                .document(CONFIG_DOC);
    }

    public AiFaqConfig find(String uid, String igAccountId) {
        try {
            DocumentSnapshot snap = document(uid, igAccountId).get().get();
            if (!snap.exists()) {
                return new AiFaqConfig();
            }
            AiFaqConfig config = snap.toObject(AiFaqConfig.class);
            return config != null ? config : new AiFaqConfig();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("find", e);
        }
    }

    public AiFaqConfig save(String uid, String igAccountId, AiFaqConfig config) {
        try {
            config.setUpdatedAt(new Date());
            document(uid, igAccountId).set(config).get();
            return config;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("AiFaqConfigRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }
}
