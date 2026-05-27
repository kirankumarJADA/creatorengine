package com.creatorengine.automation.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;
import java.util.List;

public class Automation {

    @DocumentId
    private String id;

    private String name;
    private TriggerType trigger;
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

    public boolean isEnabled() {
        return enabled;
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
            Action wrapped = new Action();
            wrapped.setType(action.getType());
            wrapped.setLink(action.getLink());
            wrapped.setMessage(message);
            return List.of(wrapped);
        }

        return List.of();
    }

    public static class Condition {
        private ConditionType type = ConditionType.ANY;
        private String keyword;
        private MatchType matchType;

        public Condition() {
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
    }

    public static class Action {
        private ActionType type = ActionType.SEND_DM;
        private String link;
        private String message;
        private Integer delaySeconds;

        public Action() {
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
    }
}