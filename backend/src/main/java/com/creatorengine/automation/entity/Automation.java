package com.creatorengine.automation.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Automation aggregate, persisted as a Firestore document under
 * {@code users/{uid}/automations/{automationId}}.
 *
 * <p>This is a plain POJO (no JPA) — Firestore's Admin SDK reflects
 * on getters/setters to (de)serialise. Nested {@link Condition} and
 * {@link Action} are also POJOs so they round-trip as map fields.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Automation {

    @DocumentId
    private String id;

    /** Human-friendly label shown on cards. May be auto-derived. */
    private String name;

    private TriggerType trigger;

    @Builder.Default
    private Condition condition = new Condition();

    @Builder.Default
    private Action action = new Action();

    /** Response message template; supports {{username}} placeholders. */
    private String message;

    /**
     * Multi-step action chain. Preferred over the legacy single
     * {@link #action} + {@link #message} pair — new automations write
     * to this list, old automations continue to load via {@link #action}
     * and are exposed through {@link #getEffectiveActions()}.
     *
     * <p>When both are present, {@code actions} wins. When the user
     * updates an automation through the new builder, the service
     * clears {@link #action} + {@link #message} so legacy fields don't
     * drift out of sync with the canonical chain.</p>
     */
    private java.util.List<Action> actions;

    /** Master toggle. False = paused. */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Anti-spam cooldown — minimum minutes between firings of this
     * automation for the same sender. Zero = no cooldown (default).
     * The cooldown layer keys on (automationId, senderInstagramUserId);
     * the same sender hitting this automation within the window is
     * silently dropped at dispatch time.
     */
    @Builder.Default
    private int cooldownMinutes = 0;

    private long runCount;
    private long successCount;

    private Instant createdAt;
    private Instant updatedAt;

    // ─── Effective view of the action chain ─────────────────
    /**
     * The execution engine's single source of truth. Returns the
     * multi-step {@link #actions} list when populated, otherwise
     * synthesises a single-element list from the legacy
     * {@link #action} + {@link #message} pair so old automations run
     * through the new sequential code path unchanged.
     */
    public java.util.List<Action> getEffectiveActions() {
        if (actions != null && !actions.isEmpty()) {
            return actions;
        }
        if (action != null) {
            Action wrapped = Action.builder()
                    .type(action.getType())
                    .link(action.getLink())
                    .message(message)
                    .build();
            return java.util.List.of(wrapped);
        }
        return java.util.List.of();
    }

    // ─── Nested ──────────────────────────────────────────────
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Condition {
        @Builder.Default
        private ConditionType type = ConditionType.ANY;
        private String keyword;
        private MatchType matchType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Action {
        @Builder.Default
        private ActionType type = ActionType.SEND_DM;
        /** Optional URL — used when type is SEND_LINK. */
        private String link;
        /** Per-action message template; supports {{username}}. SEND_MESSAGE / SEND_LINK only. */
        private String message;
        /** Wait duration in seconds. DELAY only; ignored for other action types. */
        private Integer delaySeconds;
    }
}
