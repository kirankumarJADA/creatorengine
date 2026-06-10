package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.TriggerType;

import java.time.Instant;
import java.util.List;

public record AutomationResponse(
        String id,
        String name,
        TriggerType trigger,
        String targetPostId,
        ConditionDto condition,
        ActionDto action,
        String message,
        List<ActionDto> actions,
        boolean enabled,
        int cooldownMinutes,
        boolean publicReplyEnabled,
        List<PublicReplyDto> publicReplies,
        boolean followGateEnabled,
        String followGateMessage,
        String followGateButtonLabel,
        long runCount,
        long successCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AutomationResponse from(Automation a) {
        List<ActionDto> effective = a.getEffectiveActions().stream()
                .map(ActionDto::from)
                .toList();

        List<PublicReplyDto> replies = a.getPublicReplies() == null
                ? List.of()
                : a.getPublicReplies().stream().map(PublicReplyDto::from).toList();

        return new AutomationResponse(
                a.getId(),
                a.getName(),
                a.getTrigger(),
                a.getTargetPostId(),
                ConditionDto.from(a.getCondition()),
                ActionDto.from(a.getAction()),
                a.getMessage(),
                effective,
                a.getEnabled(),
                a.getCooldownMinutes(),
                a.getPublicReplyEnabled(),
                replies,
                a.getFollowGateEnabled(),
                a.getFollowGateMessage(),
                a.getFollowGateButtonLabel(),
                a.getRunCount(),
                a.getSuccessCount(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}