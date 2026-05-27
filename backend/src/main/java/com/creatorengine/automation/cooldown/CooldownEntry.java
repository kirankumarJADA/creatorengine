package com.creatorengine.automation.cooldown;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

public class CooldownEntry {

    @DocumentId
    private String id;

    private String automationId;
    private String senderInstagramUserId;
    private Instant firedAt;
    private Instant expiresAt;

    public CooldownEntry() {
    }

    public CooldownEntry(
            String id,
            String automationId,
            String senderInstagramUserId,
            Instant firedAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.automationId = automationId;
        this.senderInstagramUserId = senderInstagramUserId;
        this.firedAt = firedAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAutomationId() {
        return automationId;
    }

    public void setAutomationId(String automationId) {
        this.automationId = automationId;
    }

    public String getSenderInstagramUserId() {
        return senderInstagramUserId;
    }

    public void setSenderInstagramUserId(String senderInstagramUserId) {
        this.senderInstagramUserId = senderInstagramUserId;
    }

    public Instant getFiredAt() {
        return firedAt;
    }

    public void setFiredAt(Instant firedAt) {
        this.firedAt = firedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}