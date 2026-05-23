package com.creatorengine.instagram.dto;

import com.creatorengine.instagram.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for the dev-only webhook simulator
 * ({@link com.creatorengine.instagram.controller.WebhookTestController}).
 *
 * Lets developers fire a fake event without going through Meta's
 * webhook plumbing — useful for end-to-end testing of event storage.
 */
public record SimulateWebhookRequest(
        @NotNull(message = "Event type is required")
        EventType type,

        @NotBlank(message = "Message is required")
        String message,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Instagram user id is required")
        String instagramUserId,

        /** Optional — only meaningful for COMMENT events. */
        String postId,

        /**
         * Optional — only meaningful for COMMENT events. Used as the
         * Private-Replies recipient when the engine sends a DM in
         * response to a comment.
         */
        String commentId,

        /** Optional — Meta's mid, used as the dedup key for DM/story-reply events. */
        String messageId,

        /** Optional — defaults to the connected user's IG account on the server side. */
        String receivingAccountId
) {}
