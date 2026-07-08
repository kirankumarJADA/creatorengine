package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        Integer delaySeconds,

        /**
         * SEND_MESSAGE / SEND_DM / SEND_LINK: optional alternate message
         * texts. When present, the executor randomly rotates between
         * `message` and each entry here on every send, so the exact same
         * text isn't sent every time. Null/empty behaves exactly as before.
         */
        @Size(max = 10, message = "At most 10 message variations")
        List<@Size(max = 2000, message = "Variation is too long (max 2000 characters)") String> variations,

        /**
         * SEND_MESSAGE / SEND_DM / SEND_LINK: optional public image URL to
         * attach to the DM (uploaded via /api/media/dm-image). Null/blank
         * means no image - unchanged behavior.
         */
        @Size(max = 1000, message = "Image URL is too long")
        String imageUrl
) {
    public Automation.Action toEntity() {
        return Automation.Action.builder()
                .type(type)
                .link(link == null ? null : link.trim())
                .message(message)
                .delaySeconds(delaySeconds)
                .variations(variations)
                .imageUrl(imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim())
                .build();
    }

    public static ActionDto from(Automation.Action a) {
        if (a == null) return new ActionDto(ActionType.SEND_DM, null, null, null, null, null);
        return new ActionDto(
                a.getType(),
                a.getMessage(),
                a.getLink(),
                a.getDelaySeconds(),
                a.getVariations(),
                a.getImageUrl()
        );
    }
}