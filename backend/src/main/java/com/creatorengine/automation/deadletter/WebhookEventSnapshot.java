package com.creatorengine.automation.deadletter;

import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;

import java.time.Instant;

public class WebhookEventSnapshot {

    private String type;
    private String message;
    private String username;
    private String instagramUserId;
    private String postId;
    private String commentId;
    private String messageId;
    private Instant eventTime;
    private String receivingAccountId;

    public WebhookEventSnapshot() {
    }

    public WebhookEventSnapshot(
            String type,
            String message,
            String username,
            String instagramUserId,
            String postId,
            String commentId,
            String messageId,
            Instant eventTime,
            String receivingAccountId
    ) {
        this.type = type;
        this.message = message;
        this.username = username;
        this.instagramUserId = instagramUserId;
        this.postId = postId;
        this.commentId = commentId;
        this.messageId = messageId;
        this.eventTime = eventTime;
        this.receivingAccountId = receivingAccountId;
    }

    public static WebhookEventSnapshotBuilder builder() {
        return new WebhookEventSnapshotBuilder();
    }

    public static WebhookEventSnapshot fromDto(WebhookEventDto e) {
        if (e == null) {
            return null;
        }

        return WebhookEventSnapshot.builder()
                .type(e.type() != null ? e.type().name() : null)
                .message(e.message())
                .username(e.username())
                .instagramUserId(e.instagramUserId())
                .postId(e.postId())
                .commentId(e.commentId())
                .messageId(e.messageId())
                .eventTime(e.eventTime())
                .receivingAccountId(e.receivingAccountId())
                .build();
    }

    public WebhookEventDto toDto() {
        EventType t;

        try {
            t = type != null ? EventType.valueOf(type) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }

        return WebhookEventDto.builder()
                .type(t)
                .message(message)
                .username(username)
                .instagramUserId(instagramUserId)
                .postId(postId)
                .commentId(commentId)
                .messageId(messageId)
                .eventTime(eventTime)
                .receivingAccountId(receivingAccountId)
                .build();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getReceivingAccountId() {
        return receivingAccountId;
    }

    public void setReceivingAccountId(String receivingAccountId) {
        this.receivingAccountId = receivingAccountId;
    }

    public static class WebhookEventSnapshotBuilder {
        private String type;
        private String message;
        private String username;
        private String instagramUserId;
        private String postId;
        private String commentId;
        private String messageId;
        private Instant eventTime;
        private String receivingAccountId;

        public WebhookEventSnapshotBuilder type(String type) {
            this.type = type;
            return this;
        }

        public WebhookEventSnapshotBuilder message(String message) {
            this.message = message;
            return this;
        }

        public WebhookEventSnapshotBuilder username(String username) {
            this.username = username;
            return this;
        }

        public WebhookEventSnapshotBuilder instagramUserId(String instagramUserId) {
            this.instagramUserId = instagramUserId;
            return this;
        }

        public WebhookEventSnapshotBuilder postId(String postId) {
            this.postId = postId;
            return this;
        }

        public WebhookEventSnapshotBuilder commentId(String commentId) {
            this.commentId = commentId;
            return this;
        }

        public WebhookEventSnapshotBuilder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public WebhookEventSnapshotBuilder eventTime(Instant eventTime) {
            this.eventTime = eventTime;
            return this;
        }

        public WebhookEventSnapshotBuilder receivingAccountId(String receivingAccountId) {
            this.receivingAccountId = receivingAccountId;
            return this;
        }

        public WebhookEventSnapshot build() {
            return new WebhookEventSnapshot(
                    type,
                    message,
                    username,
                    instagramUserId,
                    postId,
                    commentId,
                    messageId,
                    eventTime,
                    receivingAccountId
            );
        }
    }
}