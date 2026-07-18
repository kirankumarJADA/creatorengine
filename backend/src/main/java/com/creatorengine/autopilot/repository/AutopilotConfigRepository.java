package com.creatorengine.autopilot.repository;

import com.creatorengine.autopilot.entity.AutopilotConfig;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.concurrent.ExecutionException;

@Repository
public class AutopilotConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(AutopilotConfigRepository.class);
    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String AUTOPILOT = "autopilot";
    private static final String CONFIG_DOC = "config";

    private final Firestore firestore;

    public AutopilotConfigRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference doc(String uid, String igAccountId) {
        return firestore.collection(USERS).document(uid)
                .collection(ACCOUNTS).document(igAccountId)
                .collection(AUTOPILOT).document(CONFIG_DOC);
    }

    public AutopilotConfig find(String uid, String igAccountId) {
        try {
            DocumentSnapshot snap = doc(uid, igAccountId).get().get();
            if (!snap.exists()) return new AutopilotConfig();
            AutopilotConfig config = snap.toObject(AutopilotConfig.class);
            return config != null ? config : new AutopilotConfig();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to read Autopilot config uid={} igAccountId={}: {}", uid, igAccountId, e.getMessage());
            return new AutopilotConfig();
        }
    }

    public AutopilotConfig save(String uid, String igAccountId, AutopilotConfig config) {
        config.setUpdatedAt(new Date());
        try {
            doc(uid, igAccountId).set(config).get();
            return config;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save Autopilot config", e);
        }
    }
}
