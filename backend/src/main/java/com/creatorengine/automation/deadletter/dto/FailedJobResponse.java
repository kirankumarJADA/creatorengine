package com.creatorengine.automation.deadletter.dto;

import com.creatorengine.automation.deadletter.FailedJob;
import lombok.Builder;

import java.time.Instant;

/**
 * Wire shape for {@code GET /api/failed-jobs}. Exactly the fields the
 * spec asks for, with the entity's {@code createdAt} aliased as
 * {@code timestamp} since that reads more naturally in the UI.
 *
 * <p>Note that {@code automationName} is the snapshot captured at
 * dead-letter time, not a live lookup — the automation may have been
 * deleted since.</p>
 */
@Builder
public record FailedJobResponse(
        String id,
        String automationName,
        String username,
        String reason,
        int attempts,
        Instant timestamp
) {
    public static FailedJobResponse from(FailedJob job) {
        return FailedJobResponse.builder()
                .id(job.getId())
                .automationName(job.getAutomationName())
                .username(job.getUsername())
                .reason(job.getReason())
                .attempts(job.getAttempts())
                .timestamp(job.getCreatedAt())
                .build();
    }
}
