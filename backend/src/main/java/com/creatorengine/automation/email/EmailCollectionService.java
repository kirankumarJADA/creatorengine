package com.creatorengine.automation.email;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.contacts.repository.ContactRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EMAIL COLLECTION
 *
 * When an automation has emailCollectEnabled=true, this service:
 * 1. scheduleExpectation() — called after the automation sends its DM.
 *    Stores a PENDING document in `pending_email_collections` so the
 *    engine knows to watch for an email reply from this contact.
 *    The expectation expires 48 hours after the DM was sent.
 *
 * 2. tryCapture() — called by WebhookService whenever a DM arrives.
 *    If there is a PENDING expectation for this sender AND their message
 *    contains a valid email address, the email is saved to the Contact
 *    record and the expectation is marked CAPTURED.
 */
@Service
public class EmailCollectionService {

    private static final Logger log = LoggerFactory.getLogger(EmailCollectionService.class);
    private static final String COLLECTION = "pending_email_collections";

    /**
     * RFC-5321-ish email regex. Intentionally permissive — we trust the user
     * typed something that looks like an email; domain validation isn't our job.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    /** Expectation window: 48 hours in milliseconds */
    private static final long WINDOW_MILLIS = 48L * 60 * 60 * 1000;

    private final Firestore firestore;
    private final ContactRepository contactRepository;

    public EmailCollectionService(Firestore firestore, ContactRepository contactRepository) {
        this.firestore = firestore;
        this.contactRepository = contactRepository;
    }

    /**
     * Schedule (or refresh) an email-capture expectation for this contact.
     * Called after the automation's DM is successfully sent.
     */
    public void scheduleExpectation(String uid, Automation automation, WebhookEventDto event) {
        if (uid == null || automation == null || !automation.getEmailCollectEnabled()) return;
        if (event == null || event.instagramUserId() == null) return;

        String instagramUserId = event.instagramUserId();
        Date expiresAt = new Date(System.currentTimeMillis() + WINDOW_MILLIS);
        String docId = docId(uid, instagramUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("automationId", automation.getId());
        data.put("instagramUserId", instagramUserId);
        data.put("igAccountId", event.receivingAccountId() != null ? event.receivingAccountId() : "");
        data.put("status", "PENDING");
        data.put("expiresAt", expiresAt);
        data.put("updatedAt", new Date());

        try {
            firestore.collection(COLLECTION).document(docId).set(data).get();
            log.info("Email expectation scheduled uid={} ig={} expires={}",
                    uid, instagramUserId, expiresAt);
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Failed to schedule email expectation: {}", ex.getMessage());
        }
    }

    /**
     * Try to capture an email from an incoming DM.
     * Called by WebhookService whenever a DM arrives from a user.
     * No-op if there is no pending expectation or the message has no email.
     */
    public void tryCapture(String uid, String instagramUserId, String message, String igAccountId) {
        if (uid == null || instagramUserId == null || message == null || message.isBlank()) return;

        Optional<String> emailOpt = extractEmail(message);
        if (emailOpt.isEmpty()) return;  // message has no email — ignore

        String docId = docId(uid, instagramUserId);
        try {
            var snap = firestore.collection(COLLECTION).document(docId).get().get();
            if (!snap.exists()) return;

            String status = snap.getString("status");
            if (!"PENDING".equals(status)) return;

            // Check expiry
            Date expiresAt = snap.getDate("expiresAt");
            if (expiresAt != null && expiresAt.before(new Date())) {
                log.info("Email expectation expired for uid={} ig={}", uid, instagramUserId);
                snap.getReference().update("status", "EXPIRED", "updatedAt", new Date());
                return;
            }

            String resolvedIgAccountId = igAccountId != null && !igAccountId.isBlank()
                    ? igAccountId
                    : snap.getString("igAccountId");

            // Save email to contact
            String email = emailOpt.get().toLowerCase();
            contactRepository.saveEmail(uid, resolvedIgAccountId, instagramUserId, email);

            // Mark expectation as captured
            snap.getReference().update(
                    "status", "CAPTURED",
                    "capturedEmail", email,
                    "capturedAt", new Date(),
                    "updatedAt", new Date()
            ).get();

            log.info("Email captured uid={} ig={} email={}", uid, instagramUserId, email);

        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            log.warn("Failed to capture email for uid={} ig={}: {}", uid, instagramUserId, ex.getMessage());
        }
    }

    /** Extract the first email address from a text string, if any. */
    private Optional<String> extractEmail(String text) {
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    private static String docId(String uid, String instagramUserId) {
        return uid + "_" + instagramUserId;
    }
}
