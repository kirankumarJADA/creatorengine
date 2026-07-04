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

import java.util.Date; 
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class AutomationRepository {

    private static final Logger log = LoggerFactory.getLogger(AutomationRepository.class);

    private static final String USERS_COLLECTION = "users";
    private static final String AUTOMATIONS_SUBCOLLECTION = "automations";

    private final Firestore firestore;

    public AutomationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(AUTOMATIONS_SUBCOLLECTION);
    }

    private DocumentReference document(String uid, String id) {
        return collection(uid).document(id);
    }

    public List<Automation> findAllByOwner(String uid) {
        try {
            return collection(uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments().stream()
                    .map(d -> d.toObject(Automation.class))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllByOwner", e);
        }
    }

    public Optional<Automation> findById(String uid, String id) {
        if (uid == null || id == null || id.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentSnapshot snap = document(uid, id).get().get();
            return snap.exists()
                    ? Optional.ofNullable(snap.toObject(Automation.class))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findById", e);
        }
    }

    public Automation save(String uid, Automation automation) {
        try {
            Instant now = Instant.now();

            if (automation.getId() == null || automation.getId().isBlank()) {
                DocumentReference ref = collection(uid).document();
                automation.setId(ref.getId());
                automation.setCreatedAt(now);
                automation.setUpdatedAt(now);
                ref.set(automation).get();
            } else {
                automation.setUpdatedAt(now);
                document(uid, automation.getId()).set(automation).get();
            }

            return automation;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("save", e);
        }
    }

    public void deleteById(String uid, String id) {
        try {
            document(uid, id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("deleteById", e);
        }
    }

    /**
     * Cross-user scan for automations waiting on the user's next IG upload.
     * Used by NextPostLockerService.
     *
     * Each Automation is paired with its owner uid (derived from the document
     * path users/{uid}/automations/{id}), so the locker can save back via
     * save(uid, a).
     *
     * Uses Firestore collectionGroup("automations") to avoid enumerating
     * users. NOTE: this query may require a collection-group index on the
     * automations subcollection for the `targetPostMode` field. If you see a
     * Firestore "needs an index" error in the logs, click the URL Firestore
     * prints — it builds the index in ~1 minute.
     */
    public List<OwnedAutomation> findAllPendingNextPost() {
        try {
            List<QueryDocumentSnapshot> docs = firestore
                    .collectionGroup(AUTOMATIONS_SUBCOLLECTION)
                    .whereEqualTo("targetPostMode", PostTargetMode.NEXT_POST.name())
                    .get().get()
                    .getDocuments();

            List<OwnedAutomation> out = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                Automation a = doc.toObject(Automation.class);
                if (a == null) continue;

                // Skip already-locked ones.
                if (a.getTargetPostId() != null && !a.getTargetPostId().isBlank()) {
                    continue;
                }

                String uid = extractOwnerUid(doc);
                if (uid == null) {
                    log.warn("findAllPendingNextPost: could not extract uid from path={}",
                            doc.getReference().getPath());
                    continue;
                }
                out.add(new OwnedAutomation(uid, a));
            }
            return out;
        } catch (InterruptedException | ExecutionException e) {
            throw wrap("findAllPendingNextPost", e);
        }
    }

    /** Extract the parent user's uid from a doc at users/{uid}/automations/{id}. */
    private String extractOwnerUid(QueryDocumentSnapshot doc) {
        try {
            DocumentReference automationRef = doc.getReference();
            DocumentReference userRef = automationRef.getParent().getParent();
            return userRef == null ? null : userRef.getId();
        } catch (Exception e) {
            log.debug("extractOwnerUid failed: {}", e.getMessage());
            return null;
        }
    }

    private RuntimeException wrap(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("AutomationRepository.{} failed", op, e);
        return new RuntimeException("Firestore operation failed: " + op, e);
    }

    /** An automation tied to its owner's uid — needed for cross-user queries. */
    public record OwnedAutomation(String uid, Automation automation) {}
}