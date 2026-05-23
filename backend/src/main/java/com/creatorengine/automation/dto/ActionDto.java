package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Wire shape for a single action in an automation's chain.
 *
 * <p>Type-specific fields are nullable on every action — Firestore /
 * JSON couldn't carry a sealed hierarchy without custom mapping, and
 * a bag-of-optional-fields per action is the practical pattern.
 * Validation lives in {@link AutomationRequest#validate} where the
 * combination of all actions is known.</p>
 */
public record ActionDto(
        @NotNull(message = "Action type is required")
        ActionType type,

        /** SEND_MESSAGE / SEND_LINK: response template, supports {{username}}. */
        @Size(max = 2000, message = "Message is too long (max 2000 characters)")
        String message,

        /** SEND_LINK: target URL. */
        @Size(max = 500, message = "Link is too long")
        String link,

        /** DELAY: wait duration in seconds. 1 to 86400 (24h). */
        Integer delaySeconds
) {
    public Automation.Action toEntity() {
        return Automation.Action.builder()
                .type(type)
                .link(link == null ? null : link.trim())
                .message(message)
                .delaySeconds(delaySeconds)
                .build();
    }

    public static ActionDto from(Automation.Action a) {
        if (a == null) return new ActionDto(ActionType.SEND_DM, null, null, null);
        return new ActionDto(
                a.getType(),
                a.getMessage(),
                a.getLink(),
                a.getDelaySeconds()
        );
    }
}
