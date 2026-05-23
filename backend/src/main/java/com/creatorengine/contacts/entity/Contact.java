package com.creatorengine.contacts.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A person who has interacted with the user's Instagram account
 * through an automation. Stored at
 * {@code users/{uid}/contacts/{contactId}}.
 *
 * <p>The {@code instagramUserId} is the natural primary key — we
 * upsert by that field rather than letting Firestore mint random
 * doc ids, so repeat interactions don't create duplicate rows.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @DocumentId
    private String id;

    /** Instagram-scoped sender id. Stable per (user, account) tuple. */
    private String instagramUserId;

    /** @handle. May be null when Meta doesn't include it (rare for messaging events). */
    private String username;

    /** Which event type first introduced this contact (COMMENT/DM/STORY_REPLY). */
    private String source;

    /** Last message we *sent* to this contact (or for SAVE_CONTACT, the trigger text). */
    private String lastMessage;

    /**
     * Number of times this contact has triggered any automation for this user.
     * Incremented by {@code ContactRepository.upsertByInstagramUserId} on every
     * successful execution — see comment there for the invariant.
     */
    private long totalTriggers;

    private Instant createdAt;
    private Instant updatedAt;
}
