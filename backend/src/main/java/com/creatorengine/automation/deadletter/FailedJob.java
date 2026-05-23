package com.creatorengine.automation.deadletter;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted "we gave up" record at {@code users/{uid}/failed_jobs/{id}}.
 *
 * <p>One entry per terminally-failed automation execution. The fields
 * match the spec exactly; we add {@code jobId} so a single execution
 * can be cross-referenced against execution_logs and incident reports.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedJob {

    @DocumentId
    private String id;

    /** Original webhook event id (the dedup key) — null when dedup couldn't derive one. */
    private String eventId;

    private String automationId;

    /**
     * Snapshot of the automation's name at dead-letter time. Captured here
     * because the automation itself may be deleted by the time the user
     * opens the Failed Jobs page.
     */
    private String automationName;

    /**
     * Sender's IG handle (recipient of the would-be DM), surfaced in the UI.
     * Sourced from the event snapshot — kept as a separate top-level field
     * so list rendering doesn't need to dig into {@link #event}.
     */
    private String username;

    /** Either "Max retries exceeded: ..." or "Non-retryable: ...". */
    private String reason;

    private int attempts;

    /** Trace id from the queue — pairs with {@code AutomationJob.jobId}. */
    private String jobId;

    private Instant createdAt;

    /**
     * Frozen webhook event needed to reconstruct an {@code AutomationJob}
     * when the user clicks Retry. Without this, a retry would have to
     * track down the original {@code WebhookEventRecord} (which may
     * have been purged) — storing the snapshot makes retries reliable
     * for the lifetime of the failed-jobs row.
     */
    private WebhookEventSnapshot event;
}
