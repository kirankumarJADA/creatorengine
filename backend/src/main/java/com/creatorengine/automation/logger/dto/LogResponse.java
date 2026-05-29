package com.creatorengine.automation.logger.dto;

import com.creatorengine.automation.entity.ExecutionLog;
import com.creatorengine.automation.logger.ExecutionLogger;

import java.time.Instant;

public record LogResponse(
        String id,
        String username,
        String automationName,
        String triggerType,
        String eventText,
        boolean matched,
        boolean messageSent,
        String status,
        Instant timestamp
) {
    public static LogResponse from(ExecutionLog row) {
        return new LogResponse(
                row.getId(),
                row.getRecipientUsername(),
                row.getAutomationName(),
                row.getEventType(),
                row.getTriggerText(),
                row.getMatched(),
                row.getMessageSent(),
                resolveStatus(row),
                row.getTimestamp()
        );
    }

    private static String resolveStatus(ExecutionLog row) {
        if (row.getStatus() != null && !row.getStatus().isBlank()) {
            return row.getStatus();
        }

        if (!row.getMatched()) {
            return null;
        }

        if (row.getMessageSent()) {
            return ExecutionLogger.STATUS_SUCCESS;
        }

        if (row.getErrorMessage() != null) {
            return ExecutionLogger.STATUS_FAILED;
        }

        return ExecutionLogger.STATUS_SUCCESS;
    }
}