package com.creatorengine.instagram.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Persisted record of a single webhook event we received.
 *
 * <p>Stored at {@code users/{uid}/instagram_events/{eventId}} when
 * we can resolve the receiving user, or at
 * {@code orphan_webhook_events/{eventId}} when we can't (typically
 * because no CreatorEngine user is connected to the IG account
 * that fired the event — useful debugging signal).</p>
 *
 * <p>{@code rawPayload} keeps the original event JSON so we can
 * replay/diagnose later. Drop it if storage cost becomes a concern.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventRecord {

    @DocumentId
    private String id;

    private EventType type;
    private String message;        // the comment text, DM body, or reply text
    private String username;       // sender handle
    private String instagramUserId;// sender IG id
    private String postId;         // media id for comments; null for DMs
    private String commentId;      // the IG comment id for COMMENT events; null otherwise
    private String messageId;      // Meta's mid for DM / story-reply events; null otherwise
    private Instant eventTime;     // when Meta says it happened
    private Instant receivedAt;    // when our webhook saw it

    /** Original payload chunk, kept for diagnostics. */
    private Map<String, Object> rawPayload;
}
