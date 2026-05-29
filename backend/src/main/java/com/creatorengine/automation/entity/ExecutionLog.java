package com.creatorengine.automation.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

public class ExecutionLog {

    @DocumentId
    private String id;

    private String automationId;
    private boolean matched;
    private String eventType;
    private String triggerText;
    private boolean messageSent;
    private Instant timestamp;
    private String status;
    private String automationName;
    private String actionType;
    private String renderedMessage;
    private String recipientUsername;
    private String recipientInstagramId;
    private String errorMessage;
    private String metaMessageId;

    public ExecutionLog() {
    }

    public ExecutionLog(
            String id,
            String automationId,
            boolean matched,
            String eventType,
            String triggerText,
            boolean messageSent,
            Instant timestamp,
            String status,
            String automationName,
            String actionType,
            String renderedMessage,
            String recipientUsername,
            String recipientInstagramId,
            String errorMessage,
            String metaMessageId
    ) {
        this.id = id;
        this.automationId = automationId;
        this.matched = matched;
        this.eventType = eventType;
        this.triggerText = triggerText;
        this.messageSent = messageSent;
        this.timestamp = timestamp;
        this.status = status;
        this.automationName = automationName;
        this.actionType = actionType;
        this.renderedMessage = renderedMessage;
        this.recipientUsername = recipientUsername;
        this.recipientInstagramId = recipientInstagramId;
        this.errorMessage = errorMessage;
        this.metaMessageId = metaMessageId;
    }

    public static Builder builder() {
        return new Builder();
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

    public boolean getMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTriggerText() {
        return triggerText;
    }

    public void setTriggerText(String triggerText) {
        this.triggerText = triggerText;
    }

    public boolean getMessageSent() {
        return messageSent;
    }

    public void setMessageSent(boolean messageSent) {
        this.messageSent = messageSent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAutomationName() {
        return automationName;
    }

    public void setAutomationName(String automationName) {
        this.automationName = automationName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getRenderedMessage() {
        return renderedMessage;
    }

    public void setRenderedMessage(String renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getRecipientInstagramId() {
        return recipientInstagramId;
    }

    public void setRecipientInstagramId(String recipientInstagramId) {
        this.recipientInstagramId = recipientInstagramId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMetaMessageId() {
        return metaMessageId;
    }

    public void setMetaMessageId(String metaMessageId) {
        this.metaMessageId = metaMessageId;
    }

    public static final class Builder {
        private String id;
        private String automationId;
        private boolean matched;
        private String eventType;
        private String triggerText;
        private boolean messageSent;
        private Instant timestamp;
        private String status;
        private String automationName;
        private String actionType;
        private String renderedMessage;
        private String recipientUsername;
        private String recipientInstagramId;
        private String errorMessage;
        private String metaMessageId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder automationId(String automationId) {
            this.automationId = automationId;
            return this;
        }

        public Builder matched(boolean matched) {
            this.matched = matched;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder triggerText(String triggerText) {
            this.triggerText = triggerText;
            return this;
        }

        public Builder messageSent(boolean messageSent) {
            this.messageSent = messageSent;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder automationName(String automationName) {
            this.automationName = automationName;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder renderedMessage(String renderedMessage) {
            this.renderedMessage = renderedMessage;
            return this;
        }

        public Builder recipientUsername(String recipientUsername) {
            this.recipientUsername = recipientUsername;
            return this;
        }

        public Builder recipientInstagramId(String recipientInstagramId) {
            this.recipientInstagramId = recipientInstagramId;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder metaMessageId(String metaMessageId) {
            this.metaMessageId = metaMessageId;
            return this;
        }

        public ExecutionLog build() {
            return new ExecutionLog(
                    id,
                    automationId,
                    matched,
                    eventType,
                    triggerText,
                    messageSent,
                    timestamp,
                    status,
                    automationName,
                    actionType,
                    renderedMessage,
                    recipientUsername,
                    recipientInstagramId,
                    errorMessage,
                    metaMessageId
            );
        }
    }
}