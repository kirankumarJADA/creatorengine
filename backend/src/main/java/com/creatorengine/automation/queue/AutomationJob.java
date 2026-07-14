package com.creatorengine.automation.queue;

import com.creatorengine.instagram.dto.WebhookEventDto;

import java.time.Instant;
import java.util.UUID;

public record AutomationJob(
        String jobId,
        String uid,
        WebhookEventDto event,
        String automationId,
        int attempt,
        int actionIndex,
        Instant enqueuedAt,
        String lastError,
        // MULTI-ACCOUNT: the Instagram account that received the webhook event.
        // Used by AutomationEngine to load the right account credentials and
        // look up automations in the per-account Firestore path.
        String igAccountId,
        // Set to true when this job was enqueued by handleFollowGateCompletion
        // (i.e. the user tapped "I Followed"). Tells processJob to skip the
        // follow-gate check and deliver the actual content instead.
        boolean followGateCompleted
) {
    public static AutomationJob fresh(String uid, WebhookEventDto event, String automationId) {
        return new AutomationJob(
                UUID.randomUUID().toString(),
                uid,
                event,
                automationId,
                1,
                0,
                Instant.now(),
                null,
                null,
                false
        );
    }

    public AutomationJob withIgAccountId(String igAccountId) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withFollowGateCompleted(boolean followGateCompleted) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withJobId(String jobId) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withUid(String uid) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withEvent(WebhookEventDto event) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withAutomationId(String automationId) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withAttempt(int attempt) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withActionIndex(int actionIndex) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withEnqueuedAt(Instant enqueuedAt) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob withLastError(String lastError) {
        return new AutomationJob(jobId, uid, event, automationId, attempt, actionIndex, enqueuedAt, lastError, igAccountId, followGateCompleted);
    }

    public AutomationJob nextAttempt(String error) {
        return this
                .withAttempt(this.attempt + 1)
                .withLastError(error)
                .withEnqueuedAt(Instant.now());
    }

    public AutomationJob advanceTo(int nextActionIndex) {
        return this
                .withActionIndex(nextActionIndex)
                .withAttempt(1)
                .withLastError(null)
                .withEnqueuedAt(Instant.now());
    }
}