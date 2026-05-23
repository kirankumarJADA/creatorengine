package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record AutomationResponse(
        String id,
        String name,
        com.creatorengine.automation.entity.TriggerType trigger,
        ConditionDto condition,

        /** Legacy single action — populated for back-compat readers; new clients use {@link #actions}. */
        ActionDto action,

        /** Legacy single message — same back-compat note. */
        String message,

        /**
         * Canonical chain. Always populated: when the entity stores a
         * multi-step list we mirror it; when the entity only has the
         * legacy single action we synthesize a one-element list so
         * frontends can read one shape regardless of write vintage.
         */
        List<ActionDto> actions,

        boolean enabled,
        int cooldownMinutes,
        long runCount,
        long successCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AutomationResponse from(Automation a) {
        List<ActionDto> effective = a.getEffectiveActions().stream()
                .map(ActionDto::from)
                .toList();
        return AutomationResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .trigger(a.getTrigger())
                .condition(ConditionDto.from(a.getCondition()))
                .action(ActionDto.from(a.getAction()))
                .message(a.getMessage())
                .actions(effective)
                .enabled(a.isEnabled())
                .cooldownMinutes(a.getCooldownMinutes())
                .runCount(a.getRunCount())
                .successCount(a.getSuccessCount())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
