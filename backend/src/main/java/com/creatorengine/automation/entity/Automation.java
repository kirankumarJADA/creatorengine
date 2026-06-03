package com.creatorengine.automation.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;
import java.util.List;

public class Automation {

    @DocumentId
    private String id;

    private String name;
    private TriggerType trigger;
    private String targetPostId;          // NEW: IG media id this automation targets; null = all posts
    private Condition condition = new Condition();
    private Action action = new Action();
    private String message;
    private List<Action> actions;
    private boolean enabled = true;
    private int cooldownMinutes = 0;
    private long runCount;
    private long successCount;
    private Instant createdAt;
    private Instant updatedAt;

    public Automation() {
    }

    public Automation(
            String id,
            String name,
            TriggerType trigger,
            String targetPostId,
            Condition condition,
            Action action,
            String message,
            List<Action> actions,
            boolean enabled,
            int cooldownMinutes,
            long runCount,
            long successCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.trigger = trigger;
        this.targetPostId = targetPostId;
        this.condition = condition;
        this.action = action;
        this.message = message;
        this.actions = actions;
        this.enabled = enabled;
        this.cooldownMinutes = cooldownMinutes;
        this.runCount = runCount;
        this.successCount = successCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AutomationBuilder builder() {
        return new AutomationBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TriggerType getTrigger() {
        return trigger;
    }

    public void setTrigger(TriggerType trigger) {
        this.trigger = trigger;
    }

    public String getTargetPostId() {
        return targetPostId;
    }

    public void setTargetPostId(String targetPostId) {
        this.targetPostId = targetPostId;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public long getRunCount() {
        return runCount;
    }

    public void setRunCount(long runCount) {
        this.runCount = runCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Action> getEffectiveActions() {
        if (actions != null && !actions.isEmpty()) {
            return actions;
        }

        if (action != null) {
            Action wrapped = Action.builder()
                    .type(action.getType())
                    .link(action.getLink())
                    .message(message)
                    .delaySeconds(action.getDelaySeconds())
                    .build();
            return List.of(wrapped);
        }

        return List.of();
    }

    public static class AutomationBuilder {
        private String id;
        private String name;
        private TriggerType trigger;
        private String targetPostId;
        private Condition condition = new Condition();
        private Action action = new Action();
        private String message;
        private List<Action> actions;
        private boolean enabled = true;
        private int cooldownMinutes = 0;
        private long runCount;
        private long successCount;
        private Instant createdAt;
        private Instant updatedAt;

        public AutomationBuilder id(String id) {
            this.id = id;
            return this;
        }

        public AutomationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AutomationBuilder trigger(TriggerType trigger) {
            this.trigger = trigger;
            return this;
        }

        public AutomationBuilder targetPostId(String targetPostId) {
            this.targetPostId = targetPostId;
            return this;
        }

        public AutomationBuilder condition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public AutomationBuilder action(Action action) {
            this.action = action;
            return this;
        }

        public AutomationBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AutomationBuilder actions(List<Action> actions) {
            this.actions = actions;
            return this;
        }

        public AutomationBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public AutomationBuilder cooldownMinutes(int cooldownMinutes) {
            this.cooldownMinutes = cooldownMinutes;
            return this;
        }

        public AutomationBuilder runCount(long runCount) {
            this.runCount = runCount;
            return this;
        }

        public AutomationBuilder successCount(long successCount) {
            this.successCount = successCount;
            return this;
        }

        public AutomationBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AutomationBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Automation build() {
            return new Automation(
                    id,
                    name,
                    trigger,
                    targetPostId,
                    condition,
                    action,
                    message,
                    actions,
                    enabled,
                    cooldownMinutes,
                    runCount,
                    successCount,
                    createdAt,
                    updatedAt
            );
        }
    }

    public static class Condition {
        private ConditionType type = ConditionType.ANY;
        private String keyword;
        private MatchType matchType;

        public Condition() {
        }

        public Condition(ConditionType type, String keyword, MatchType matchType) {
            this.type = type;
            this.keyword = keyword;
            this.matchType = matchType;
        }

        public static ConditionBuilder builder() {
            return new ConditionBuilder();
        }

        public ConditionType getType() {
            return type;
        }

        public void setType(ConditionType type) {
            this.type = type;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public MatchType getMatchType() {
            return matchType;
        }

        public void setMatchType(MatchType matchType) {
            this.matchType = matchType;
        }

        public static class ConditionBuilder {
            private ConditionType type = ConditionType.ANY;
            private String keyword;
            private MatchType matchType;

            public ConditionBuilder type(ConditionType type) {
                this.type = type;
                return this;
            }

            public ConditionBuilder keyword(String keyword) {
                this.keyword = keyword;
                return this;
            }

            public ConditionBuilder matchType(MatchType matchType) {
                this.matchType = matchType;
                return this;
            }

            public Condition build() {
                return new Condition(type, keyword, matchType);
            }
        }
    }

    public static class Action {
        private ActionType type = ActionType.SEND_DM;
        private String link;
        private String message;
        private Integer delaySeconds;

        public Action() {
        }

        public Action(ActionType type, String link, String message, Integer delaySeconds) {
            this.type = type;
            this.link = link;
            this.message = message;
            this.delaySeconds = delaySeconds;
        }

        public static ActionBuilder builder() {
            return new ActionBuilder();
        }

        public ActionType getType() {
            return type;
        }

        public void setType(ActionType type) {
            this.type = type;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getDelaySeconds() {
            return delaySeconds;
        }

        public void setDelaySeconds(Integer delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        public static class ActionBuilder {
            private ActionType type = ActionType.SEND_DM;
            private String link;
            private String message;
            private Integer delaySeconds;

            public ActionBuilder type(ActionType type) {
                this.type = type;
                return this;
            }

            public ActionBuilder link(String link) {
                this.link = link;
                return this;
            }

            public ActionBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ActionBuilder delaySeconds(Integer delaySeconds) {
                this.delaySeconds = delaySeconds;
                return this;
            }

            public Action build() {
                return new Action(type, link, message, delaySeconds);
            }
        }
    }
}