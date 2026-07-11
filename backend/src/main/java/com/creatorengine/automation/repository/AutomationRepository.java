package com.creatorengine.automation.repository;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.PostTargetMode;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * MULTI-ACCOUNT: Automations are now scoped per Instagram account.
 *
 * New path:    users/{uid}/accounts/{igAccountId}/automations/{id}
 * Legacy path: users/{uid}/automations/{id}
 *
 * All methods that take igAccountId use the new path.
 * Legacy methods (without igAccountId) are kept for backward compat
 * during the migration period and will be removed in a future release.
 */
@Repository
public class AutomationRepository {

    private static final Logger log = LoggerFactory.getLogger(AutomationRepository.class);

    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String AUTOMATIONS = "automations";

    private final Firestore firestore;

    public AutomationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    // ─── New multi-account paths ──────────────────────────────────

    private CollectionReference collection(String uid, String igAccountId) {
        return firestore.collection(USERS)
                .document(uid)
                .collection(ACCOUNTS)
                .document(igAccountId)
                .collection(AUTOMATIONS);
    }

    private DocumentReference document(String uid, String igAccountId, String id) {
        return collection(uid, igAccountId).document(id);
    }

    public List<Automation> findAllByOwner(String uid, String igAccountId) {
        try {
            return collection(uid, igAccountId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Automation.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllByOwner", e);
        }
    }

    public Optional<Automation> findById(String uid, String igAccountId, String id) {
        if (uid == null || igAccountId == null || id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            DocumentSnapshot snap = document(uid, igAccountId, id).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(Automation.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById", e);
        }
    }

    public Automation save(String uid, String igAccountId, Automation automation) {
        try {
            Date now = new Date();
            if (automation.getId() == null || automation.getId().isBlank()) {
                DocumentReference ref = collection(uid, igAccountId).document();
                automation.setId(ref.getId());
                automation.setCreatedAt(now);
                automation.setUpdatedAt(now);
                ref.set(automation).get();
            } else {
                automation.setUpdatedAt(now);
                document(uid, igAccountId, automation.getId()).set(automation).get();
            }
            return automation;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public void deleteById(String uid, String igAccountId, String id) {
        try {
            document(uid, igAccountId, id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById", e);
        }
    }

    // ─── Cross-account queries (collectionGroup) ──────────────────

    public List<OwnedAutomation> findAllAcrossUsers() {
        try {
            List<QueryDocumentSnapshot> docs = firestore
                    .collectionGroup(AUTOMATIONS)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments();

            List<OwnedAutomation> out = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                Automation a = doc.toObject(Automation.class);
                if (a == null) continue;
                String[] uidAndIgId = extractUidAndIgId(doc);
                if (uidAndIgId == null) continue;
                out.add(new OwnedAutomation(uidAndIgId[0], uidAndIgId[1], a));
            }
            return out;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllAcrossUsers", e);
        }
    }

    public List<OwnedAutomation> findAllPendingNextPost() {
        try {
            List<QueryDocumentSnapshot> docs = firestore
                    .collectionGroup(AUTOMATIONS)
                    .whereEqualTo("targetPostMode", PostTargetMode.NEXT_POST.name())
                    .get().get()
                    .getDocuments();

            List<OwnedAutomation> out = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                Automation a = doc.toObject(Automation.class);
                if (a == null) continue;
                if (a.getTargetPostId() != null && !a.getTargetPostId().isBlank()) continue;

                String[] uidAndIgId = extractUidAndIgId(doc);
                if (uidAndIgId == null) {
                    log.warn("findAllPendingNextPost: could not extract uid/igId from path={}",
                            doc.getReference().getPath());
                    continue;
                }
                out.add(new OwnedAutomation(uidAndIgId[0], uidAndIgId[1], a));
            }
            return out;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllPendingNextPost", e);
        }
    }

    // ─── Legacy single-account methods (backward compat) ──────────

    /**
     * @deprecated Use findAllByOwner(uid, igAccountId) instead.
     */
    @Deprecated
    public List<Automation> findAllByOwner(String uid) {
        try {
            return firestore.collection(USERS)
                    .document(uid)
                    .collection(AUTOMATIONS)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Automation.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllByOwner(legacy)", e);
        }
    }

    /**
     * @deprecated Use findById(uid, igAccountId, id) instead.
     */
    @Deprecated
    public Optional<Automation> findById(String uid, String id) {
        if (uid == null || id == null || id.isBlank()) return Optional.empty();
        try {
            DocumentSnapshot snap = firestore.collection(USERS)
                    .document(uid)
                    .collection(AUTOMATIONS)
                    .document(id)
                    .get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(Automation.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById(legacy)", e);
        }
    }

    /**
     * @deprecated Use save(uid, igAccountId, automation) instead.
     */
    @Deprecated
    public Automation save(String uid, Automation automation) {
        try {
            Date now = new Date();
            CollectionReference col = firestore.collection(USERS)
                    .document(uid).collection(AUTOMATIONS);
            if (automation.getId() == null || automation.getId().isBlank()) {
                DocumentReference ref = col.document();
                automation.setId(ref.getId());
                automation.setCreatedAt(now);
                automation.setUpdatedAt(now);
                ref.set(automation).get();
            } else {
                automation.setUpdatedAt(now);
                col.document(automation.getId()).set(automation).get();
            }
            return automation;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save(legacy)", e);
        }
    }

    /**
     * @deprecated Use deleteById(uid, igAccountId, id) instead.
     */
    @Deprecated
    public void deleteById(String uid, String id) {
        try {
            firestore.collection(USERS).document(uid)
                    .collection(AUTOMATIONS).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById(legacy)", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────

    /**
     * Extract [uid, igAccountId] from a collectionGroup document path.
     * New path: users/{uid}/accounts/{igAccountId}/automations/{id}
     * Legacy path: users/{uid}/automations/{id} → returns [uid, null]
     */
    private String[] extractUidAndIgId(QueryDocumentSnapshot doc) {
        try {
            // automations/{id} → parent = accounts/{igAccountId} or users/{uid}
            DocumentReference parent = doc.getReference().getParent().getParent();
            if (parent == null) return null;

            String parentCollectionId = parent.getParent().getId();

            if (ACCOUNTS.equals(parentCollectionId)) {
                // New path: users/{uid}/accounts/{igAccountId}
                String igAccountId = parent.getId();
                String uid = parent.getParent().getParent().getId();
                return new String[]{uid, igAccountId};
            } else {
                // Legacy path: users/{uid}/automations/{id} (uid is the parent)
                return new String[]{parent.getId(), null};
            }
        } catch (Exception e) {
            log.debug("extractUidAndIgId failed: {}", e.getMessage());
            return null;
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.error("AutomationRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    public record OwnedAutomation(String uid, String igAccountId, Automation automation) {}
}