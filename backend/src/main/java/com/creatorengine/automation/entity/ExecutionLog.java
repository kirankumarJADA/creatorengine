package com.creatorengine.automation.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Row in the per-user execution-log subcollection at
 * {@code users/{uid}/execution_logs/{id}}.
 *
 * <p>One row per automation that fires (i.e. {@code matched == true}).
 * We don't log misses — with a chatty Instagram account that would
 * dwarf the matched logs and bloat storage.</p>
 *
 * <p>Beyond the spec-required fields we also store {@code errorMessage}
 * (when sending failed), {@code recipientUsername}, and the
 * automation's snapshot name — purely diagnostic, all optional.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

    @DocumentId
    private String id;

    // ─── Spec-required fields ────────────────────────────────
    private String automationId;
    private boolean matched;
    private String eventType;       // COMMENT / DM / STORY_REPLY
    private String triggerText;     // the incoming message content
    private boolean messageSent;
    private Instant timestamp;

    /**
     * Outcome of this log row — one of SUCCESS / FAILED /
     * COOLDOWN_SKIPPED / DUPLICATE_IGNORED.
     *
     * <p>Stored as a plain String rather than an enum so historical rows
     * (which predate this field) deserialise cleanly as null; the
     * activity-logs API derives a status from {@code matched} and
     * {@code messageSent} when the field is missing.</p>
     */
    private String status;

    // ─── Diagnostics (optional) ──────────────────────────────
    private String automationName;
    private String actionType;       // SEND_DM / SEND_MESSAGE / ...
    private String renderedMessage;  // exact text that hit the wire
    private String recipientUsername;
    private String recipientInstagramId;
    private String errorMessage;     // populated when messageSent == false
    private String metaMessageId;    // Meta's mid, when present
}
