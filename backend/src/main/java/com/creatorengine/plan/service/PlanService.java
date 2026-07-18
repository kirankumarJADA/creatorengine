package com.creatorengine.plan.service;

import com.creatorengine.plan.entity.Plan;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * Manages user plan/subscription tier.
 * Plan is stored at: users/{uid}/plan/current (single doc)
 *
 * No payment system yet — plan defaults to FREE.
 * To upgrade a user: call setPlan(uid, Plan.PRO) from an admin
 * endpoint or Stripe webhook handler (not built yet).
 */
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    private static final String USERS = "users";
    private static final String PLAN_SUBCOLLECTION = "plan";
    private static final String PLAN_DOC = "current";

    private final Firestore firestore;

    public PlanService(Firestore firestore) {
        this.firestore = firestore;
    }

    public Plan getPlan(String uid) {
        try {
            DocumentSnapshot snap = firestore.collection(USERS)
                    .document(uid)
                    .collection(PLAN_SUBCOLLECTION)
                    .document(PLAN_DOC)
                    .get().get();

            if (!snap.exists()) {
                log.info("getPlan uid={} — doc does not exist, returning FREE", uid);
                return Plan.FREE;
            }

            String planName = snap.getString("plan");
            log.info("getPlan uid={} — planName={} rawData={}", uid, planName, snap.getData());
            if (planName == null) return Plan.FREE;

            try {
                return Plan.valueOf(planName);
            } catch (IllegalArgumentException e) {
                log.warn("getPlan uid={} — unknown plan value '{}', returning FREE", uid, planName);
                return Plan.FREE;
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to read plan for uid={}, defaulting to FREE: {}", uid, e.getMessage());
            return Plan.FREE;
        }
    }

    public void setPlan(String uid, Plan plan) {
        try {
            firestore.collection(USERS)
                    .document(uid)
                    .collection(PLAN_SUBCOLLECTION)
                    .document(PLAN_DOC)
                    .set(java.util.Map.of("plan", plan.name()))
                    .get();
            log.info("Plan set to {} for uid={}", plan, uid);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to set plan for uid={}: {}", uid, e.getMessage());
        }
    }

    public boolean canAddAccount(String uid, int currentAccountCount) {
        Plan plan = getPlan(uid);
        return currentAccountCount < plan.maxInstagramAccounts();
    }

    public int maxAccounts(String uid) {
        return getPlan(uid).maxInstagramAccounts();
    }
}