package com.creatorengine.automation.deadletter;

import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Firestore-serializable mirror of {@link WebhookEventDto}.
 *
 * <p>Embedded on {@link FailedJob} so that a "Retry" action can
 * reconstruct the original event and re-enqueue it through the normal
 * dispatch path. {@code WebhookEventDto} itself is a {@code record},
 * which Firestore's POJO mapper can't deserialize (no no-arg
 * constructor + setters) — hence this parallel class.</p>
 *
 * <p>{@code type} is stored as a String rather than {@code EventType}
 * so legacy or malformed rows degrade gracefully on read.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventSnapshot {

    private String type;             // EventType.name(), or null
    private String message;
    private String username;
    private String instagramUserId;
    private String postId;
    private String commentId;
    private String messageId;
    private Instant eventTime;
    private String receivingAccountId;

    public static WebhookEventSnapshot fromDto(WebhookEventDto e) {
        if (e == null) return null;
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

    /**
     * Best-effort rehydration. If {@code type} doesn't map to a known
     * {@link EventType} (newer event-type added since the row was
     * written), we return null and the caller handles "can't retry".
     */
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
}
