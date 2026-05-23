package com.creatorengine.instagram.dto;

import com.creatorengine.instagram.entity.EventType;
import lombok.Builder;

import java.time.Instant;

/**
 * Normalised webhook event shape. The {@link com.creatorengine.instagram.service.WebhookEventParser}
 * flattens Meta's nested entry/change/messaging schemas into one of these
 * per event, regardless of which webhook field fired.
 */
@Builder
public record WebhookEventDto(
        EventType type,
        String message,
        String username,
        String instagramUserId,
        String postId,
        /**
         * The Instagram comment id, populated only for COMMENT events.
         * Required by Meta's "Private Replies" API — to DM someone who
         * commented (without them having messaged us first) the recipient
         * must be {@code {"comment_id": <commentId>}}, not their user id.
         */
        String commentId,
        /**
         * Meta's {@code mid} for DM / story-reply events. Used as the
         * deduplication key — Meta resends webhooks aggressively and
         * the dedup layer keys on this (or {@code commentId} for
         * comments). Null for COMMENT events.
         */
        String messageId,
        Instant eventTime,
        /** The IG business account id this event was delivered to (i.e. who owns the inbox). */
        String receivingAccountId
) {
    /**
     * Stable identity for deduplication. Prefixed by event kind so
     * a comment and a DM with coincidentally identical ids never collide.
     * Returns null when no usable id is present (which means the event
     * effectively cannot be deduplicated and will always be processed).
     */
    public String dedupKey() {
        if (type == null) return null;
        return switch (type) {
            case COMMENT     -> commentId != null ? "c:" + commentId : null;
            case DM,
                 STORY_REPLY -> messageId != null ? "m:" + messageId : null;
        };
    }
}
