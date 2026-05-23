package com.creatorengine.automation.logger.dto;

import com.creatorengine.automation.entity.ExecutionLog;
import com.creatorengine.automation.logger.ExecutionLogger;
import lombok.Builder;

import java.time.Instant;

/**
 * Wire shape for {@code GET /api/logs}. Fields exactly match the spec.
 *
 * <p>{@code status} carries forward whatever the entity has, falling
 * back to a derivation for old rows that predate the field: a
 * historical {@code matched && messageSent} row becomes SUCCESS;
 * a historical {@code matched && !messageSent && errorMessage}
 * becomes FAILED. No old row was ever written with a missing match,
 * so we don't need to handle COOLDOWN_SKIPPED / DUPLICATE_IGNORED
 * in the fallback path.</p>
 */
@Builder
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
        return LogResponse.builder()
                .id(row.getId())
                .username(row.getRecipientUsername())
                .automationName(row.getAutomationName())
                .triggerType(row.getEventType())
                .eventText(row.getTriggerText())
                .matched(row.isMatched())
                .messageSent(row.isMessageSent())
                .status(resolveStatus(row))
                .timestamp(row.getTimestamp())
                .build();
    }

    private static String resolveStatus(ExecutionLog row) {
        // Modern row — explicit status.
        if (row.getStatus() != null && !row.getStatus().isBlank()) {
            return row.getStatus();
        }
        // Legacy row — derive from booleans + error.
        if (!row.isMatched()) return null;
        if (row.isMessageSent()) return ExecutionLogger.STATUS_SUCCESS;
        if (row.getErrorMessage() != null) return ExecutionLogger.STATUS_FAILED;
        // SAVE_CONTACT-style match that succeeded without sending.
        return ExecutionLogger.STATUS_SUCCESS;
    }
}
