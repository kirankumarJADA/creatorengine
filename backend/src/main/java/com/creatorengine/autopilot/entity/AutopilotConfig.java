package com.creatorengine.autopilot.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Per-Instagram-account AI Autopilot configuration (#15).
 * Stored at: users/{uid}/accounts/{igAccountId}/autopilot/config
 *
 * Unlike AI FAQ (one-off Q&A), Autopilot holds full multi-turn
 * conversations, collects lead data, and can take actions. Pro/Agency
 * plan only. Takes priority over AI FAQ for a given account when enabled
 * so the two features never both answer the same message.
 */
public class AutopilotConfig {

    private boolean enabled = false;
    private AutopilotRole role = AutopilotRole.SALES_ASSISTANT;
    private String systemPrompt = "";
    private String goal = "";
    private String tone = "friendly";
    private AllowedActions allowedActions = new AllowedActions();
    private int conversationTimeoutMinutes = 30;
    private String fallbackMessage =
            "Thanks for reaching out! I'll have someone from the team follow up with you shortly.";
    private List<MessageTemplate> messageTemplates = new ArrayList<>();
    private List<String> allowedAutomationIds = new ArrayList<>();
    private Date updatedAt;

    public AutopilotConfig() {}

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public AutopilotRole getRole() { return role; }
    public void setRole(AutopilotRole role) { this.role = role; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public AllowedActions getAllowedActions() { return allowedActions; }
    public void setAllowedActions(AllowedActions allowedActions) {
        this.allowedActions = allowedActions != null ? allowedActions : new AllowedActions();
    }

    public int getConversationTimeoutMinutes() { return conversationTimeoutMinutes; }
    public void setConversationTimeoutMinutes(int v) { this.conversationTimeoutMinutes = v; }

    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String fallbackMessage) { this.fallbackMessage = fallbackMessage; }

    public List<MessageTemplate> getMessageTemplates() { return messageTemplates; }
    public void setMessageTemplates(List<MessageTemplate> messageTemplates) {
        this.messageTemplates = messageTemplates != null ? messageTemplates : new ArrayList<>();
    }

    public List<String> getAllowedAutomationIds() { return allowedAutomationIds; }
    public void setAllowedAutomationIds(List<String> allowedAutomationIds) {
        this.allowedAutomationIds = allowedAutomationIds != null ? allowedAutomationIds : new ArrayList<>();
    }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /** True if there's enough configured to actually run a conversation. */
    public boolean hasContent() {
        return (systemPrompt != null && !systemPrompt.isBlank())
                || (goal != null && !goal.isBlank());
    }
}
