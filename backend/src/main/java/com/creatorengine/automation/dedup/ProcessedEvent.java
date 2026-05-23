package com.creatorengine.automation.dedup;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Record at {@code processed_events/{dedupKey}}.
 *
 * <p>Existence of the document is the dedup signal — the body is just
 * for diagnostics + TTL. {@code expiresAt} is set 7 days ahead; enable
 * Firestore's TTL policy on this field in the console to let the
 * collection self-prune.</p>
 *
 * <p>7 days is comfortably longer than Meta's webhook redelivery window
 * but short enough that the collection doesn't grow unboundedly.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @DocumentId
    private String id;            // the dedup key itself (e.g. "c:17841...")

    private String eventType;     // COMMENT / DM / STORY_REPLY
    private String uid;           // owner who received the event
    private Instant processedAt;
    /** TTL anchor — Firestore prunes documents past this point if TTL is enabled. */
    private Instant expiresAt;
}
