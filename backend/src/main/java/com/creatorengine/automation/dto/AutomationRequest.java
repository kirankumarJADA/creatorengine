package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ConditionType;
import com.creatorengine.automation.entity.PostTargetMode;
import com.creatorengine.exception.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AutomationRequest(
        @Size(max = 120, message = "Name is too long")
        String name,

        @NotNull(message = "Trigger is required")
        com.creatorengine.automation.entity.TriggerType trigger,

        /** How to scope this automation to posts. Null = inferred (ALL / SPECIFIC from targetPostId). */
        PostTargetMode targetPostMode,

        String targetPostId,

        @NotNull(message = "Condition is required")
        @Valid ConditionDto condition,

        @Valid ActionDto action,

        @Size(max = 2000, message = "Message is too long (max 2000 characters)")
        String message,

        @Size(max = 10, message = "An automation can contain at most 10 actions")
        @Valid List<ActionDto> actions,

        Boolean enabled,

        Integer cooldownMinutes,

        Boolean publicReplyEnabled,

        @Size(max = 10, message = "At most 10 public replies")
        @Valid List<PublicReplyDto> publicReplies,

        Boolean followGateEnabled,

        @Size(max = 1000, message = "Follow message is too long (max 1000 characters)")
        String followGateMessage,

        @Size(max = 20, message = "Button label is too long (max 20 characters)")
        String followGateButtonLabel
) {

    private static final int MIN_DELAY_SECONDS = 1;
    private static final int MAX_DELAY_SECONDS = 24 * 60 * 60;

    public void validate() {
        if (condition.type() == ConditionType.KEYWORD) {
            if (condition.keyword() == null || condition.keyword().isBlank()) {
                throw new BadRequestException("Keyword is required when condition type is KEYWORD.");
            }
            if (condition.matchType() == null) {
                throw new BadRequestException("Match type is required when condition type is KEYWORD.");
            }
        }

        PostTargetMode resolvedMode = resolveTargetPostMode();
        if (resolvedMode == PostTargetMode.SPECIFIC
                && (targetPostId == null || targetPostId.isBlank())) {
            throw new BadRequestException("Pick a post when target mode is SPECIFIC.");
        }
        if (resolvedMode == PostTargetMode.NEXT_POST
                && targetPostId != null && !targetPostId.isBlank()) {
            // NEXT_POST should never arrive with a pre-set post id from the client.
            throw new BadRequestException("NEXT_POST automations cannot pre-select a post.");
        }

        if (actions != null && !actions.isEmpty()) {
            validateChain();
        } else {
            validateLegacyAction();
        }

        if (Boolean.TRUE.equals(publicReplyEnabled)) {
            boolean anyActive = publicReplies != null && publicReplies.stream()
                    .anyMatch(r -> r != null
                            && r.text() != null && !r.text().isBlank()
                            && (r.enabled() == null || r.enabled()));
            if (!anyActive) {
                throw new BadRequestException(
                        "Add at least one active public reply, or turn off public replies.");
            }
        }

        if (Boolean.TRUE.equals(followGateEnabled)
                && (followGateMessage == null || followGateMessage.isBlank())) {
            throw new BadRequestException(
                    "Add a follow message, or turn off 'Ask to follow first'.");
        }
    }

    private PostTargetMode resolveTargetPostMode() {
        if (targetPostMode != null) return targetPostMode;
        return (targetPostId != null && !targetPostId.isBlank())
                ? PostTargetMode.SPECIFIC
                : PostTargetMode.ALL;
    }

    private void validateChain() {
        if (actions.size() > 10) {
            throw new BadRequestException("An automation can contain at most 10 actions.");
        }
        boolean anyEffective = actions.stream().anyMatch(a -> a.type() != ActionType.DELAY);
        if (!anyEffective) {
            throw new BadRequestException("At least one action must do something (not just DELAY).");
        }

        for (int i = 0; i < actions.size(); i++) {
            ActionDto a = actions.get(i);
            if (a == null || a.type() == null) {
                throw new BadRequestException("Action " + (i + 1) + " has no type.");
            }
            int human = i + 1;
            switch (a.type()) {
                case SEND_LINK:
                    if (a.link() == null || a.link().isBlank()) {
                        throw new BadRequestException("Action " + human + ": link is required for SEND_LINK.");
                    }
                    if (a.message() == null || a.message().isBlank()) {
                        throw new BadRequestException("Action " + human + ": message is required for SEND_LINK.");
                    }
                    break;
                case SEND_MESSAGE:
                case SEND_DM:
                    if (a.message() == null || a.message().isBlank()) {
                        throw new BadRequestException("Action " + human + ": message is required.");
                    }
                    break;
                case DELAY:
                    Integer secs = a.delaySeconds();
                    if (secs == null || secs < MIN_DELAY_SECONDS) {
                        throw new BadRequestException(
                                "Action " + human + ": delay must be at least " + MIN_DELAY_SECONDS + " second(s).");
                    }
                    if (secs > MAX_DELAY_SECONDS) {
                        throw new BadRequestException(
                                "Action " + human + ": delay can be at most 24 hours (" + MAX_DELAY_SECONDS + "s).");
                    }
                    break;
                case SAVE_CONTACT:
                    break;
            }
        }
    }

    private void validateLegacyAction() {
        if (action == null || action.type() == null) {
            throw new BadRequestException("Action is required.");
        }
        if (action.type() == ActionType.SEND_LINK
                && (action.link() == null || action.link().isBlank())) {
            throw new BadRequestException("Link is required when action type is SEND_LINK.");
        }
        if (action.type() != ActionType.SAVE_CONTACT
                && action.type() != ActionType.DELAY
                && (message == null || message.isBlank())) {
            throw new BadRequestException("Response message is required for this action.");
        }
        if (action.type() == ActionType.DELAY) {
            throw new BadRequestException("DELAY must be used inside a multi-step actions[] chain.");
        }
    }

    public Automation toEntity() {
        PostTargetMode resolvedMode = resolveTargetPostMode();

        Automation.AutomationBuilder builder = Automation.builder()
                .name(name == null ? null : name.trim())
                .trigger(trigger)
                .targetPostMode(resolvedMode)
                .targetPostId(resolvedMode == PostTargetMode.NEXT_POST ? null : targetPostId)
                .condition(condition.toEntity())
                .enabled(enabled == null || enabled)
                .cooldownMinutes(cooldownMinutes == null ? 0 : Math.max(0, Math.min(cooldownMinutes, 24 * 60)))
                .publicReplyEnabled(Boolean.TRUE.equals(publicReplyEnabled))
                .followGateEnabled(Boolean.TRUE.equals(followGateEnabled))
                .followGateMessage(followGateMessage == null ? null : followGateMessage.trim())
                .followGateButtonLabel(
                        followGateButtonLabel == null || followGateButtonLabel.isBlank()
                                ? "I Followed ✅"
                                : followGateButtonLabel.trim());

        if (publicReplies != null) {
            builder.publicReplies(publicReplies.stream()
                    .filter(r -> r != null && r.text() != null && !r.text().isBlank())
                    .map(PublicReplyDto::toEntity)
                    .toList());
        }

        if (actions != null && !actions.isEmpty()) {
            builder.actions(actions.stream().map(ActionDto::toEntity).toList());
            builder.action(null);
            builder.message(null);
        } else {
            builder.action(action.toEntity());
            builder.message(message == null ? null : message.trim());
        }
        return builder.build();
    }
}