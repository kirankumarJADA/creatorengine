package com.creatorengine.contacts.dto;

import com.creatorengine.contacts.entity.Contact;
import lombok.Builder;

import java.time.Instant;

/**
 * Wire shape returned by {@code GET /api/contacts}.
 *
 * <p>Fields are exactly what the frontend Contacts table renders.
 * Note that {@code lastInteraction} maps from the entity's
 * {@code updatedAt} — they're conceptually the same thing (every
 * successful execution touches {@code updatedAt}) but the public name
 * reads better.</p>
 *
 * <p>{@code lastMessage} is deliberately omitted — the table doesn't
 * show it, and it's per-contact PII that doesn't need to ride along.</p>
 */
@Builder
public record ContactResponse(
        String id,
        String username,
        String instagramUserId,
        String source,
        long totalTriggers,
        Instant lastInteraction,
        Instant createdAt
) {
    public static ContactResponse from(Contact c) {
        return ContactResponse.builder()
                .id(c.getId())
                .username(c.getUsername())
                .instagramUserId(c.getInstagramUserId())
                .source(c.getSource())
                .totalTriggers(c.getTotalTriggers())
                .lastInteraction(c.getUpdatedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
