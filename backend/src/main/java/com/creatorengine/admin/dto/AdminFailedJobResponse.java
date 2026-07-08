package com.creatorengine.admin.dto;

import com.creatorengine.automation.deadletter.FailedJob;

import java.time.Instant;

public record AdminFailedJobResponse(
        String id,
        String uid,
        String ownerEmail,
        String automationName,
        String username,
        String reason,
        int attempts,
        Instant timestamp
) {
    public static AdminFailedJobResponse from(String uid, String ownerEmail, FailedJob job) {
        return new AdminFailedJobResponse(
                job.getId(),
                uid,
                ownerEmail,
                job.getAutomationName(),
                job.getUsername(),
                job.getReason(),
                job.getAttempts(),
                job.getCreatedAt()
        );
    }
}