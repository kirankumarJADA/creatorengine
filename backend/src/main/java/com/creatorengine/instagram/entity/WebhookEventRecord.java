package com.creatorengine.instagram.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;
import java.util.Map;

public class WebhookEventRecord {

    @DocumentId
    private String id;

    private EventType type;
    private String message;
    private String username;
    private String instagramUserId;
    private String postId;
    private String commentId;
    private String messageId;
    private Instant eventTime;
    private Instant receivedAt;
    private Map<String, Object> rawPayload;

    public WebhookEventRecord() {
    }

    public static WebhookEventRecordBuilder builder() {
        return new WebhookEventRecordBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInstagramUserId() {
        return instagramUserId;
    }

    public void setInstagramUserId(String instagramUserId) {
        this.instagramUserId = instagramUserId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Map<String, Object> getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(Map<String, Object> rawPayload) {
        this.rawPayload = rawPayload;
    }

    public static class WebhookEventRecordBuilder {
        private final WebhookEventRecord record = new WebhookEventRecord();

        public WebhookEventRecordBuilder id(String id) {
            record.id = id;
            return this;
        }

        public WebhookEventRecordBuilder type(EventType type) {
            record.type = type;
            return this;
        }

        public WebhookEventRecordBuilder message(String message) {
            record.message = message;
            return this;
        }

        public WebhookEventRecordBuilder username(String username) {
            record.username = username;
            return this;
        }

        public WebhookEventRecordBuilder instagramUserId(String instagramUserId) {
            record.instagramUserId = instagramUserId;
            return this;
        }

        public WebhookEventRecordBuilder postId(String postId) {
            record.postId = postId;
            return this;
        }

        public WebhookEventRecordBuilder commentId(String commentId) {
            record.commentId = commentId;
            return this;
        }

        public WebhookEventRecordBuilder messageId(String messageId) {
            record.messageId = messageId;
            return this;
        }

        public WebhookEventRecordBuilder eventTime(Instant eventTime) {
            record.eventTime = eventTime;
            return this;
        }

        public WebhookEventRecordBuilder receivedAt(Instant receivedAt) {
            record.receivedAt = receivedAt;
            return this;
        }

        public WebhookEventRecordBuilder rawPayload(Map<String, Object> rawPayload) {
            record.rawPayload = rawPayload;
            return this;
        }

        public WebhookEventRecord build() {
            return record;
        }
    }
}