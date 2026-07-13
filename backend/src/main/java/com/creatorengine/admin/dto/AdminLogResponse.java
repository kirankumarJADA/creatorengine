package com.creatorengine.admin.dto;

import com.creatorengine.automation.entity.ExecutionLog;

import java.time.Instant;

public record AdminLogResponse(
        String id,
        String uid,
        String ownerEmail,
        String username,
        String automationName,
        String eventType,
        String triggerText,
        boolean matched,
        boolean messageSent,
        String status,
        Instant timestamp
) {
    public static AdminLogResponse from(String uid, String ownerEmail, ExecutionLog row) {
        String status = row.getStatus() != null
                ? row.getStatus()
                : (row.getMessageSent() ? "SUCCESS" : (row.getMatched() ? "FAILED" : null));

        return new AdminLogResponse(
                row.getId(),
                uid,
                ownerEmail,
                row.getRecipientUsername(),
                row.getAutomationName(),
                row.getEventType(),
                row.getTriggerText(),
                row.getMatched(),
                row.getMessageSent(),
                status,
                row.getTimestamp()
        );
    }
}