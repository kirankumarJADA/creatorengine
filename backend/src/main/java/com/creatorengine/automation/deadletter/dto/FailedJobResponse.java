package com.creatorengine.automation.deadletter.dto;

import com.creatorengine.automation.deadletter.FailedJob;

import java.time.Instant;

public record FailedJobResponse(
        String id,
        String automationName,
        String username,
        String reason,
        int attempts,
        Instant timestamp
) {
    public static FailedJobResponse from(FailedJob job) {
        return new FailedJobResponse(
                job.getId(),
                job.getAutomationName(),
                job.getUsername(),
                job.getReason(),
                job.getAttempts(),
                job.getCreatedAt()
        );
    }
}