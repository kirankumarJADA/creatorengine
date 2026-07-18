package com.creatorengine.autopilot.dto;

public record AutopilotStatsResponse(
        boolean enabled,
        String aiModel,
        long conversationCount,
        long avgResponseTimeMs,
        long contactsHandled,
        long escalations,
        long qualifiedLeads
) {
}
