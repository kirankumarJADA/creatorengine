package com.creatorengine.automation.dedup;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

public class ProcessedEvent {

    @DocumentId
    private String id;

    private String eventType;
    private String uid;
    private Instant processedAt;
    private Instant expiresAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String id, String eventType, String uid, Instant processedAt, Instant expiresAt) {
        this.id = id;
        this.eventType = eventType;
        this.uid = uid;
        this.processedAt = processedAt;
        this.expiresAt = expiresAt;
    }

    public static ProcessedEventBuilder builder() {
        return new ProcessedEventBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public static class ProcessedEventBuilder {
        private String id;
        private String eventType;
        private String uid;
        private Instant processedAt;
        private Instant expiresAt;

        public ProcessedEventBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ProcessedEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public ProcessedEventBuilder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public ProcessedEventBuilder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public ProcessedEventBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public ProcessedEvent build() {
            return new ProcessedEvent(id, eventType, uid, processedAt, expiresAt);
        }
    }
}