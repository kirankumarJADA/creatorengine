package com.creatorengine.admin.dto;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.TriggerType;

import java.util.Date;

public record AdminAutomationResponse(
        String id,
        String uid,
        String ownerEmail,
        String name,
        TriggerType trigger,
        boolean enabled,
        long runCount,
        long successCount,
        Date createdAt,
        Date updatedAt
) {
    public static AdminAutomationResponse from(String uid, String ownerEmail, Automation a) {
        return new AdminAutomationResponse(
                a.getId(),
                uid,
                ownerEmail,
                a.getName(),
                a.getTrigger(),
                a.getEnabled(),
                a.getRunCount(),
                a.getSuccessCount(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}