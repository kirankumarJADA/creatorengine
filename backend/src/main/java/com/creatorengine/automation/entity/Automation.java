package com.creatorengine.automation.entity;

import java.util.Date;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;

import java.util.List;

public class Automation {

    @DocumentId
    private String id;

    private String name;
    private TriggerType trigger;
    private PostTargetMode targetPostMode;
    private String targetPostId;
    private List<String> baselineMediaIds;
    private Date nextPostLockedAt;
    private Condition condition = new Condition();
    private Action action = new Action();
    private String message;
    private List<Action> actions;
    private boolean enabled = true;
    private int cooldownMinutes = 0;
    private boolean publicReplyEnabled = false;
    private List<PublicReply> publicReplies;
    private boolean followGateEnabled = false;
    private String followGateMessage;
    private String followGateButtonLabel;
    private long runCount;
    private long successCount;
    private Date createdAt;
    private Date updatedAt;

    public Automation() {}

    public Automation(
            String id, String name, TriggerType trigger,
            PostTargetMode targetPostMode, String targetPostId,
            List<String> baselineMediaIds, Date nextPostLockedAt,
            Condition condition, Action action, String message,
            List<Action> actions, boolean enabled, int cooldownMinutes,
            boolean publicReplyEnabled, List<PublicReply> publicReplies,
            boolean followGateEnabled, String followGateMessage,
            String followGateButtonLabel, long runCount, long successCount,
            Date createdAt, Date updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.trigger = trigger;
        this.targetPostMode = targetPostMode;
        this.targetPostId = targetPostId;
        this.baselineMediaIds = baselineMediaIds;
        this.nextPostLockedAt = nextPostLockedAt;
        this.condition = condition;
        this.action = action;
        this.message = message;
        this.actions = actions;
        this.enabled = enabled;
        this.cooldownMinutes = cooldownMinutes;
        this.publicReplyEnabled = publicReplyEnabled;
        this.publicReplies = publicReplies;
        this.followGateEnabled = followGateEnabled;
        this.followGateMessage = followGateMessage;
        this.followGateButtonLabel = followGateButtonLabel;
        this.runCount = runCount;
        this.successCount = successCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AutomationBuilder builder() { return new AutomationBuilder(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TriggerType getTrigger() { return trigger; }
    public void setTrigger(TriggerType trigger) { this.trigger = trigger; }
    public PostTargetMode getTargetPostMode() { return targetPostMode; }
    public void setTargetPostMode(PostTargetMode targetPostMode) { this.targetPostMode = targetPostMode; }
    public String getTargetPostId() { return targetPostId; }
    public void setTargetPostId(String targetPostId) { this.targetPostId = targetPostId; }
    public List<String> getBaselineMediaIds() { return baselineMediaIds; }
    public void setBaselineMediaIds(List<String> baselineMediaIds) { this.baselineMediaIds = baselineMediaIds; }
    public Date getNextPostLockedAt() { return nextPostLockedAt; }
    public void setNextPostLockedAt(Date nextPostLockedAt) { this.nextPostLockedAt = nextPostLockedAt; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Action> getActions() { return actions; }
    public void setActions(List<Action> actions) { this.actions = actions; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public boolean getPublicReplyEnabled() { return publicReplyEnabled; }
    public void setPublicReplyEnabled(boolean publicReplyEnabled) { this.publicReplyEnabled = publicReplyEnabled; }
    public List<PublicReply> getPublicReplies() { return publicReplies; }
    public void setPublicReplies(List<PublicReply> publicReplies) { this.publicReplies = publicReplies; }
    public boolean getFollowGateEnabled() { return followGateEnabled; }
    public void setFollowGateEnabled(boolean followGateEnabled) { this.followGateEnabled = followGateEnabled; }
    public String getFollowGateMessage() { return followGateMessage; }
    public void setFollowGateMessage(String followGateMessage) { this.followGateMessage = followGateMessage; }
    public String getFollowGateButtonLabel() { return followGateButtonLabel; }
    public void setFollowGateButtonLabel(String followGateButtonLabel) { this.followGateButtonLabel = followGateButtonLabel; }
    public long getRunCount() { return runCount; }
    public void setRunCount(long runCount) { this.runCount = runCount; }
    public long getSuccessCount() { return successCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Exclude
    public PostTargetMode getEffectiveTargetPostMode() {
        if (targetPostMode != null) return targetPostMode;
        return (targetPostId == null || targetPostId.isBlank())
                ? PostTargetMode.ALL
                : PostTargetMode.SPECIFIC;
    }

    @Exclude
    public List<Action> getEffectiveActions() {
        if (actions != null && !actions.isEmpty()) return actions;
        if (action != null && hasLegacyPayload()) {
            Action wrapped = Action.builder()
                    .type(action.getType())
                    .link(action.getLink())
                    .message(message)
                    .delaySeconds(action.getDelaySeconds())
                    .variations(action.getVariations())
                    .build();
            return List.of(wrapped);
        }
        return List.of();
    }

    @Exclude
    private boolean hasLegacyPayload() {
        boolean hasMessage = message != null && !message.isBlank();
        boolean hasLink = action != null && action.getLink() != null && !action.getLink().isBlank();
        boolean hasVariations = action != null && action.getVariations() != null && !action.getVariations().isEmpty();
        return hasMessage || hasLink || hasVariations;
    }

    public static class AutomationBuilder {
        private String id;
        private String name;
        private TriggerType trigger;
        private PostTargetMode targetPostMode;
        private String targetPostId;
        private List<String> baselineMediaIds;
        private Date nextPostLockedAt;
        private Condition condition = new Condition();
        private Action action = new Action();
        private String message;
        private List<Action> actions;
        private boolean enabled = true;
        private int cooldownMinutes = 0;
        private boolean publicReplyEnabled = false;
        private List<PublicReply> publicReplies;
        private boolean followGateEnabled = false;
        private String followGateMessage;
        private String followGateButtonLabel;
        private long runCount;
        private long successCount;
        private Date createdAt;
        private Date updatedAt;

        public AutomationBuilder id(String id) { this.id = id; return this; }
        public AutomationBuilder name(String name) { this.name = name; return this; }
        public AutomationBuilder trigger(TriggerType trigger) { this.trigger = trigger; return this; }
        public AutomationBuilder targetPostMode(PostTargetMode targetPostMode) { this.targetPostMode = targetPostMode; return this; }
        public AutomationBuilder targetPostId(String targetPostId) { this.targetPostId = targetPostId; return this; }
        public AutomationBuilder baselineMediaIds(List<String> baselineMediaIds) { this.baselineMediaIds = baselineMediaIds; return this; }
        public AutomationBuilder nextPostLockedAt(Date nextPostLockedAt) { this.nextPostLockedAt = nextPostLockedAt; return this; }
        public AutomationBuilder condition(Condition condition) { this.condition = condition; return this; }
        public AutomationBuilder action(Action action) { this.action = action; return this; }
        public AutomationBuilder message(String message) { this.message = message; return this; }
        public AutomationBuilder actions(List<Action> actions) { this.actions = actions; return this; }
        public AutomationBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public AutomationBuilder cooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; return this; }
        public AutomationBuilder publicReplyEnabled(boolean publicReplyEnabled) { this.publicReplyEnabled = publicReplyEnabled; return this; }
        public AutomationBuilder publicReplies(List<PublicReply> publicReplies) { this.publicReplies = publicReplies; return this; }
        public AutomationBuilder followGateEnabled(boolean followGateEnabled) { this.followGateEnabled = followGateEnabled; return this; }
        public AutomationBuilder followGateMessage(String followGateMessage) { this.followGateMessage = followGateMessage; return this; }
        public AutomationBuilder followGateButtonLabel(String followGateButtonLabel) { this.followGateButtonLabel = followGateButtonLabel; return this; }
        public AutomationBuilder runCount(long runCount) { this.runCount = runCount; return this; }
        public AutomationBuilder successCount(long successCount) { this.successCount = successCount; return this; }
        public AutomationBuilder createdAt(Date createdAt) { this.createdAt = createdAt; return this; }
        public AutomationBuilder updatedAt(Date updatedAt) { this.updatedAt = updatedAt; return this; }

        public Automation build() {
            return new Automation(
                    id, name, trigger,
                    targetPostMode, targetPostId, baselineMediaIds, nextPostLockedAt,
                    condition, action, message, actions,
                    enabled, cooldownMinutes,
                    publicReplyEnabled, publicReplies,
                    followGateEnabled, followGateMessage, followGateButtonLabel,
                    runCount, successCount, createdAt, updatedAt
            );
        }
    }

    public static class Condition {
        private ConditionType type = ConditionType.ANY;
        private String keyword;
        private MatchType matchType;

        public Condition() {}
        public Condition(ConditionType type, String keyword, MatchType matchType) {
            this.type = type;
            this.keyword = keyword;
            this.matchType = matchType;
        }

        public static ConditionBuilder builder() { return new ConditionBuilder(); }
        public ConditionType getType() { return type; }
        public void setType(ConditionType type) { this.type = type; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public MatchType getMatchType() { return matchType; }
        public void setMatchType(MatchType matchType) { this.matchType = matchType; }

        public static class ConditionBuilder {
            private ConditionType type = ConditionType.ANY;
            private String keyword;
            private MatchType matchType;

            public ConditionBuilder type(ConditionType type) { this.type = type; return this; }
            public ConditionBuilder keyword(String keyword) { this.keyword = keyword; return this; }
            public ConditionBuilder matchType(MatchType matchType) { this.matchType = matchType; return this; }
            public Condition build() { return new Condition(type, keyword, matchType); }
        }
    }

    public static class Action {
        private ActionType type = ActionType.SEND_DM;
        private String link;
        private String message;
        private Integer delaySeconds;

        // ---------------------------------------------------------------
        // NEW: Message Variations
        // Optional list of alternate message texts. When present (2+ items),
        // ActionExecutor randomly picks ONE of these instead of the single
        // `message` field each time the action runs. This avoids sending the
        // exact same text on every send, which helps reduce the chance of
        // Instagram flagging the account for repetitive/bot-like behavior.
        // If `variations` is null/empty, behavior is unchanged - `message` is
        // used as before. Fully backward compatible.
        // ---------------------------------------------------------------
        private List<String> variations;

        public Action() {}
        public Action(ActionType type, String link, String message, Integer delaySeconds) {
            this.type = type;
            this.link = link;
            this.message = message;
            this.delaySeconds = delaySeconds;
        }

        public Action(ActionType type, String link, String message, Integer delaySeconds, List<String> variations) {
            this.type = type;
            this.link = link;
            this.message = message;
            this.delaySeconds = delaySeconds;
            this.variations = variations;
        }

        public static ActionBuilder builder() { return new ActionBuilder(); }
        public ActionType getType() { return type; }
        public void setType(ActionType type) { this.type = type; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getDelaySeconds() { return delaySeconds; }
        public void setDelaySeconds(Integer delaySeconds) { this.delaySeconds = delaySeconds; }
        public List<String> getVariations() { return variations; }
        public void setVariations(List<String> variations) { this.variations = variations; }

        public static class ActionBuilder {
            private ActionType type = ActionType.SEND_DM;
            private String link;
            private String message;
            private Integer delaySeconds;
            private List<String> variations;

            public ActionBuilder type(ActionType type) { this.type = type; return this; }
            public ActionBuilder link(String link) { this.link = link; return this; }
            public ActionBuilder message(String message) { this.message = message; return this; }
            public ActionBuilder delaySeconds(Integer delaySeconds) { this.delaySeconds = delaySeconds; return this; }
            public ActionBuilder variations(List<String> variations) { this.variations = variations; return this; }
            public Action build() { return new Action(type, link, message, delaySeconds, variations); }
        }
    }

    public static class PublicReply {
        private String text;
        private boolean enabled = true;

        public PublicReply() {}
        public PublicReply(String text, boolean enabled) {
            this.text = text;
            this.enabled = enabled;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public boolean getEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}