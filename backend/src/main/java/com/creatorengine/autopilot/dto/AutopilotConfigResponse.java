package com.creatorengine.autopilot.dto;

import com.creatorengine.autopilot.entity.AllowedActions;
import com.creatorengine.autopilot.entity.AutopilotConfig;
import com.creatorengine.autopilot.entity.AutopilotRole;
import com.creatorengine.autopilot.entity.MessageTemplate;

import java.util.List;

public record AutopilotConfigResponse(
        boolean enabled,
        AutopilotRole role,
        String systemPrompt,
        String goal,
        String tone,
        AllowedActions allowedActions,
        int conversationTimeoutMinutes,
        String fallbackMessage,
        List<MessageTemplate> messageTemplates,
        List<String> allowedAutomationIds,
        boolean planEligible,
        String plan
) {
    public static AutopilotConfigResponse from(AutopilotConfig config, boolean planEligible, String plan) {
        return new AutopilotConfigResponse(
                config.getEnabled(),
                config.getRole(),
                config.getSystemPrompt(),
                config.getGoal(),
                config.getTone(),
                config.getAllowedActions(),
                config.getConversationTimeoutMinutes(),
                config.getFallbackMessage(),
                config.getMessageTemplates(),
                config.getAllowedAutomationIds(),
                planEligible,
                plan
        );
    }
}
