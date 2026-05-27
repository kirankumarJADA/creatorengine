package com.creatorengine.automation.deadletter;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

public class FailedJob {

    @DocumentId
    private String id;

    private String eventId;
    private String automationId;
    private String automationName;
    private String username;
    private String reason;
    private int attempts;
    private String jobId;
    private Instant createdAt;
    private WebhookEventSnapshot event;

    public FailedJob() {
    }

    public static FailedJobBuilder builder() {
        return new FailedJobBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAutomationId() {
        return automationId;
    }

    public void setAutomationId(String automationId) {
        this.automationId = automationId;
    }

    public String getAutomationName() {
        return automationName;
    }

    public void setAutomationName(String automationName) {
        this.automationName = automationName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public WebhookEventSnapshot getEvent() {
        return event;
    }

    public void setEvent(WebhookEventSnapshot event) {
        this.event = event;
    }

    public static class FailedJobBuilder {
        private String eventId;
        private String automationId;
        private String automationName;
        private String username;
        private String reason;
        private int attempts;
        private String jobId;
        private Instant createdAt;
        private WebhookEventSnapshot event;

        public FailedJobBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public FailedJobBuilder automationId(String automationId) {
            this.automationId = automationId;
            return this;
        }

        public FailedJobBuilder automationName(String automationName) {
            this.automationName = automationName;
            return this;
        }

        public FailedJobBuilder username(String username) {
            this.username = username;
            return this;
        }

        public FailedJobBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public FailedJobBuilder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public FailedJobBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public FailedJobBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FailedJobBuilder event(WebhookEventSnapshot event) {
            this.event = event;
            return this;
        }

        public FailedJob build() {
            FailedJob job = new FailedJob();
            job.eventId = eventId;
            job.automationId = automationId;
            job.automationName = automationName;
            job.username = username;
            job.reason = reason;
            job.attempts = attempts;
            job.jobId = jobId;
            job.createdAt = createdAt;
            job.event = event;
            return job;
        }
    }
}