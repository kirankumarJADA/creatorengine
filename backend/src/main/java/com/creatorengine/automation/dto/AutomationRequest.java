package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ConditionType;
import com.creatorengine.exception.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for POST (create) and PUT (update).
 *
 * <p>Cross-field rules — "keyword required when condition.type is
 * KEYWORD", per-action "link required for SEND_LINK", per-action
 * "message required except for SAVE_CONTACT and DELAY", "delaySeconds
 * required for DELAY" — are enforced by {@link #validate()} which the
 * service layer calls before persistence.</p>
 *
 * <p>Multi-step support: requests may carry either the legacy
 * {@link #action} + {@link #message} pair (single-step automations
 * built by the old wizard) or the new {@link #actions} list (multi-step
 * chain from the upgraded builder). {@link #toEntity()} reconciles
 * both and writes to the canonical {@code actions} field; legacy
 * fields are cleared whenever a chain is provided.</p>
 */
public record AutomationRequest(
        @Size(max = 120, message = "Name is too long")
        String name,

        @NotNull(message = "Trigger is required")
        com.creatorengine.automation.entity.TriggerType trigger,

        /**
         * IG media id this automation applies to. Null = all posts.
         * Only meaningful for comment triggers; ignored for DM triggers.
         */
        String targetPostId,

        @NotNull(message = "Condition is required")
        @Valid ConditionDto condition,

        /**
         * Legacy single-action field. Kept for back-compat — the
         * upgraded builder sends {@link #actions} instead. May be null
         * when {@code actions} is provided.
         */
        @Valid ActionDto action,

        /** Legacy single-message template — paired with {@link #action}. */
        @Size(max = 2000, message = "Message is too long (max 2000 characters)")
        String message,

        /**
         * Multi-step chain (1..10 actions). When present, takes
         * precedence over {@link #action} + {@link #message}.
         */
        @Size(max = 10, message = "An automation can contain at most 10 actions")
        @Valid List<ActionDto> actions,

        Boolean enabled,

        /** Anti-spam cooldown in minutes. Null/0 = no cooldown. Capped at 24 hours. */
        Integer cooldownMinutes,

        /** Master toggle: also post a public reply on the triggering comment. */
        Boolean publicReplyEnabled,

        /**
         * Public reply templates (max 10). One *active* template is chosen
         * at random per triggering comment.
         */
        @Size(max = 10, message = "At most 10 public replies")
        @Valid List<PublicReplyDto> publicReplies
) {

    /** Hard caps on DELAY action duration. */
    private static final int MIN_DELAY_SECONDS = 1;
    private static final int MAX_DELAY_SECONDS = 24 * 60 * 60; // 24h

    public void validate() {
        // Keyword condition still needs keyword + matchType regardless of action shape
        if (condition.type() == ConditionType.KEYWORD) {
            if (condition.keyword() == null || condition.keyword().isBlank()) {
                throw new BadRequestException("Keyword is required when condition type is KEYWORD.");
            }
            if (condition.matchType() == null) {
                throw new BadRequestException("Match type is required when condition type is KEYWORD.");
            }
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
    }

    private void validateChain() {
        if (actions.size() > 10) {
            throw new BadRequestException("An automation can contain at most 10 actions.");
        }
        // Reject a chain that's all delays — has to actually do something user-visible.
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
                    // Nothing required.
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
            // Legacy single-action with DELAY is a degenerate config — reject rather than
            // try to make sense of "a delay that does nothing else".
            throw new BadRequestException("DELAY must be used inside a multi-step actions[] chain.");
        }
    }

    public Automation toEntity() {
        Automation.AutomationBuilder builder = Automation.builder()
                .name(name == null ? null : name.trim())
                .trigger(trigger)
                .targetPostId(targetPostId)
                .condition(condition.toEntity())
                .enabled(enabled == null || enabled)
                .cooldownMinutes(cooldownMinutes == null ? 0 : Math.max(0, Math.min(cooldownMinutes, 24 * 60)))
                .publicReplyEnabled(Boolean.TRUE.equals(publicReplyEnabled));

        if (publicReplies != null) {
            builder.publicReplies(publicReplies.stream()
                    .filter(r -> r != null && r.text() != null && !r.text().isBlank())
                    .map(PublicReplyDto::toEntity)
                    .toList());
        }

        if (actions != null && !actions.isEmpty()) {
            // New chain — write to actions[] and clear legacy fields so they
            // don't drift out of sync.
            builder.actions(actions.stream().map(ActionDto::toEntity).toList());
            builder.action(null);
            builder.message(null);
        } else {
            // Legacy single-action — write to the legacy pair.
            builder.action(action.toEntity());
            builder.message(message == null ? null : message.trim());
        }
        return builder.build();
    }
}