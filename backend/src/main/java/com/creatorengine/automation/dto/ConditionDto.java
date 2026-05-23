package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ConditionType;
import com.creatorengine.automation.entity.MatchType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Wire format for the condition leg of an automation.
 *
 * Cross-field validation (keyword required when type==KEYWORD) lives
 * in {@link AutomationRequest#validate()} so the message reads well.
 */
public record ConditionDto(
        @NotNull(message = "Condition type is required")
        ConditionType type,

        @Size(max = 80, message = "Keyword is too long")
        String keyword,

        MatchType matchType
) {
    public Automation.Condition toEntity() {
        return Automation.Condition.builder()
                .type(type)
                .keyword(keyword == null ? null : keyword.trim())
                .matchType(matchType)
                .build();
    }

    public static ConditionDto from(Automation.Condition c) {
        if (c == null) return new ConditionDto(ConditionType.ANY, null, null);
        return new ConditionDto(c.getType(), c.getKeyword(), c.getMatchType());
    }
}
