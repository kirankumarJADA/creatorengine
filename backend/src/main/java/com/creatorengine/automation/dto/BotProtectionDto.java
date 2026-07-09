package com.creatorengine.automation.dto;

/**
 * Wire shape for bot protection settings on an automation.
 * Sent from frontend → backend on create/update, and back on read.
 */
public record BotProtectionDto(
        boolean enabled,
        int minDelaySeconds,
        int maxDelaySeconds
) {
    /** Sensible defaults when bot protection section is absent from payload */
    public static BotProtectionDto defaults() {
        return new BotProtectionDto(false, 2, 8);
    }
}