package com.creatorengine.admin.dto;

import java.util.List;

public record DashboardStatsResponse(
        long totalUsers,
        long activeUsersToday,
        long activeUsers7d,
        long instagramConnected,
        long totalAutomations,
        long activeAutomations,
        long totalDmsSent,
        long failedJobsCount,
        List<AdminLogResponse> recentActivity
) {}