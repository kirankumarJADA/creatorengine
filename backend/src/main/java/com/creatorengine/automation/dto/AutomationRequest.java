package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ConditionType;
import com.creatorengine.automation.entity.FollowUpDelayUnit;
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
        String followGateButtonLabel,

        // BOT PROTECTION fields — all optional, safe defaults applied in toEntity()
        Boolean botProtectionEnabled,
        Integer botProtectionMinDelaySeconds,
        Integer botProtectionMaxDelaySeconds,

        // FOLLOW-UP MESSAGE fields — single no-reply follow-up
        Boolean followUpEnabled,
        Integer followUpDelayAmount,
        FollowUpDelayUnit followUpDelayUnit,

        @Size(max = 2000, message = "Follow-up message is too long (max 2000 characters)")
        String followUpMessage,

        // EMAIL COLLECTION fields
        Boolean emailCollectEnabled,

        @Size(max = 2000, message = "Email collect message is too long (max 2000 characters)")
        String emailCollectMessage
) {

    private static final int MIN_DELAY_SECONDS = 1;
    private static final int MAX_DELAY_SECONDS = 24 * 60 * 60;
    private static final int BOT_MIN_JITTER = 0;
    private static final int BOT_MAX_JITTER = 60;

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
            throw new BadRequestException("NEXT_POST automations cannot pre-select a post.");
        }

        boolean hasActions = actions != null && !actions.isEmpty();
        boolean hasLegacyAction = action != null && action.type() != null;
        boolean hasPublicReply = Boolean.TRUE.equals(publicReplyEnabled);
        boolean hasFollowGate = Boolean.TRUE.equals(followGateEnabled);

        if (!hasActions && !hasLegacyAction && !hasPublicReply && !hasFollowGate) {
            throw new BadRequestException(
                    "Add a DM, a public reply, or a follow gate — the automation has to do something.");
        }

        if (hasActions) {
            validateChain();
        } else if (hasLegacyAction) {
            validateLegacyAction();
        }

        if (hasPublicReply) {
            boolean anyActive = publicReplies != null && publicReplies.stream()
                    .anyMatch(r -> r != null
                            && r.text() != null && !r.text().isBlank()
                            && (r.enabled() == null || r.enabled()));
            if (!anyActive) {
                throw new BadRequestException(
                        "Add at least one active public reply, or turn off public replies.");
            }
        }

        if (hasFollowGate
                && (followGateMessage == null || followGateMessage.isBlank())) {
            throw new BadRequestException(
                    "Add a follow message, or turn off 'Ask to follow first'.");
        }

        // Validate bot protection delay range if provided
        if (Boolean.TRUE.equals(botProtectionEnabled)) {
            int min = botProtectionMinDelaySeconds != null ? botProtectionMinDelaySeconds : 2;
            int max = botProtectionMaxDelaySeconds != null ? botProtectionMaxDelaySeconds : 8;
            if (min < BOT_MIN_JITTER || min > BOT_MAX_JITTER) {
                throw new BadRequestException("Bot protection min delay must be between 0 and 60 seconds.");
            }
            if (max < min || max > BOT_MAX_JITTER) {
                throw new BadRequestException("Bot protection max delay must be >= min and <= 60 seconds.");
            }
        }

        // Validate follow-up message settings if enabled
        if (Boolean.TRUE.equals(followUpEnabled)) {
            if (followUpMessage == null || followUpMessage.isBlank()) {
                throw new BadRequestException(
                        "Add a follow-up message, or turn off the follow-up.");
            }
            if (followUpDelayAmount == null || followUpDelayAmount <= 0) {
                throw new BadRequestException(
                        "Follow-up delay must be greater than 0.");
            }
            if (followUpDelayUnit == null) {
                throw new BadRequestException(
                        "Pick a unit (Minutes, Hours, or Days) for the follow-up delay.");
            }
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
                    break;
                case SEND_MESSAGE:
                case SEND_DM:
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
        if (action == null || action.type() == null) return;
        if (action.type() == ActionType.SEND_LINK
                && (action.link() == null || action.link().isBlank())) {
            throw new BadRequestException("Link is required when action type is SEND_LINK.");
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
                                : followGateButtonLabel.trim())
                .botProtectionEnabled(Boolean.TRUE.equals(botProtectionEnabled))
                .botProtectionMinDelaySeconds(
                        botProtectionMinDelaySeconds != null
                                ? Math.max(BOT_MIN_JITTER, Math.min(botProtectionMinDelaySeconds, BOT_MAX_JITTER))
                                : 2)
                .botProtectionMaxDelaySeconds(
                        botProtectionMaxDelaySeconds != null
                                ? Math.max(BOT_MIN_JITTER, Math.min(botProtectionMaxDelaySeconds, BOT_MAX_JITTER))
                                : 8)
                .followUpEnabled(Boolean.TRUE.equals(followUpEnabled))
                .followUpDelayAmount(followUpDelayAmount != null ? Math.max(1, followUpDelayAmount) : 1)
                .followUpDelayUnit(followUpDelayUnit != null ? followUpDelayUnit : FollowUpDelayUnit.HOURS)
                .followUpMessage(followUpMessage == null ? null : followUpMessage.trim())
                .emailCollectEnabled(Boolean.TRUE.equals(emailCollectEnabled))
                .emailCollectMessage(emailCollectMessage == null ? null : emailCollectMessage.trim());

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
        } else if (action != null && action.type() != null) {
            builder.action(action.toEntity());
            builder.message(message == null ? null : message.trim());
        }
        return builder.build();
    }
}