package com.creatorengine.contacts.dto;

import com.creatorengine.contacts.entity.Contact;

import java.time.Instant;

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
        return new ContactResponse(
                c.getId(),
                c.getUsername(),
                c.getInstagramUserId(),
                c.getSource(),
                c.getTotalTriggers(),
                c.getUpdatedAt(),
                c.getCreatedAt()
        );
    }
}