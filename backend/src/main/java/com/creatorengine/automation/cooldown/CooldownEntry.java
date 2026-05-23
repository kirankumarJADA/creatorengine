package com.creatorengine.automation.cooldown;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Per-(automation, sender) firing record at
 * {@code users/{uid}/cooldowns/{automationId}:{senderInstagramUserId}}.
 *
 * <p>{@code expiresAt} is set to {@code firedAt + cooldownMinutes}.
 * Firestore TTL can be wired to it for automatic cleanup, but it's
 * also fine to leave them around — the collection is bounded by
 * (automations × distinct senders) which is small in practice.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooldownEntry {

    @DocumentId
    private String id;

    private String automationId;
    private String senderInstagramUserId;
    private Instant firedAt;
    private Instant expiresAt;
}
