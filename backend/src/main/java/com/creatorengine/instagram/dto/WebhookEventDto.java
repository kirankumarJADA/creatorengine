package com.creatorengine.instagram.dto;

import com.creatorengine.instagram.entity.EventType;

import java.time.Instant;

public record WebhookEventDto(
        EventType type,
        String message,
        String username,
        String instagramUserId,
        String postId,
        String commentId,
        String messageId,
        String quickReplyPayload,
        Instant eventTime,
        String receivingAccountId
) {
    public static WebhookEventDtoBuilder builder() {
        return new WebhookEventDtoBuilder();
    }

    public String dedupKey() {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case COMMENT -> commentId != null ? "c:" + commentId : null;
            case DM, STORY_REPLY, CONTENT_SHARED -> messageId != null ? "m:" + messageId : null;
        };
    }

    public static class WebhookEventDtoBuilder {
        private EventType type;
        private String message;
        private String username;
        private String instagramUserId;
        private String postId;
        private String commentId;
        private String messageId;
        private String quickReplyPayload;
        private Instant eventTime;
        private String receivingAccountId;

        public WebhookEventDtoBuilder type(EventType type) {
            this.type = type;
            return this;
        }

        public WebhookEventDtoBuilder message(String message) {
            this.message = message;
            return this;
        }

        public WebhookEventDtoBuilder username(String username) {
            this.username = username;
            return this;
        }

        public WebhookEventDtoBuilder instagramUserId(String instagramUserId) {
            this.instagramUserId = instagramUserId;
            return this;
        }

        public WebhookEventDtoBuilder postId(String postId) {
            this.postId = postId;
            return this;
        }

        public WebhookEventDtoBuilder commentId(String commentId) {
            this.commentId = commentId;
            return this;
        }

        public WebhookEventDtoBuilder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public WebhookEventDtoBuilder quickReplyPayload(String quickReplyPayload) {
            this.quickReplyPayload = quickReplyPayload;
            return this;
        }

        public WebhookEventDtoBuilder eventTime(Instant eventTime) {
            this.eventTime = eventTime;
            return this;
        }

        public WebhookEventDtoBuilder receivingAccountId(String receivingAccountId) {
            this.receivingAccountId = receivingAccountId;
            return this;
        }

        public WebhookEventDto build() {
            return new WebhookEventDto(
                    type,
                    message,
                    username,
                    instagramUserId,
                    postId,
                    commentId,
                    messageId,
                    quickReplyPayload,
                    eventTime,
                    receivingAccountId
            );
        }
    }
}