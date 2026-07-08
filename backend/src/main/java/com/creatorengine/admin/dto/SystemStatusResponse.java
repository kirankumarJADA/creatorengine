package com.creatorengine.admin.dto;

public record SystemStatusResponse(
        String apiStatus,
        String databaseStatus,
        String webhookConfigured,
        long activeUsersLast30Min,
        long totalDmsSent,
        String renderDashboardUrl
) {}