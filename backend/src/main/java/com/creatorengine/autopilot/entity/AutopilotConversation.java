package com.creatorengine.autopilot.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One ongoing (or completed) Autopilot conversation with a single
 * Instagram contact. Stored at:
 * users/{uid}/accounts/{igAccountId}/autopilotConversations/{instagramUserId}
 */
public class AutopilotConversation {

    @DocumentId
    private String id; // instagramUserId

    private String uid;
    private String igAccountId;
    private String instagramUserId;
    private String username;

    private List<AutopilotMessage> messages = new ArrayList<>();

    private String collectedName;
    private String collectedEmail;
    private String collectedPhone;
    private String collectedPreferences;
    private String collectedBudget;

    private boolean qualified = false;
    private boolean escalated = false;

    private Date startedAt;
    private Date lastMessageAt;
    private int messageCount = 0;
    private long totalResponseTimeMs = 0;
    private int responseCount = 0;

    public AutopilotConversation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getIgAccountId() { return igAccountId; }
    public void setIgAccountId(String igAccountId) { this.igAccountId = igAccountId; }

    public String getInstagramUserId() { return instagramUserId; }
    public void setInstagramUserId(String instagramUserId) { this.instagramUserId = instagramUserId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<AutopilotMessage> getMessages() { return messages; }
    public void setMessages(List<AutopilotMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public String getCollectedName() { return collectedName; }
    public void setCollectedName(String collectedName) { this.collectedName = collectedName; }

    public String getCollectedEmail() { return collectedEmail; }
    public void setCollectedEmail(String collectedEmail) { this.collectedEmail = collectedEmail; }

    public String getCollectedPhone() { return collectedPhone; }
    public void setCollectedPhone(String collectedPhone) { this.collectedPhone = collectedPhone; }

    public String getCollectedPreferences() { return collectedPreferences; }
    public void setCollectedPreferences(String collectedPreferences) { this.collectedPreferences = collectedPreferences; }

    public String getCollectedBudget() { return collectedBudget; }
    public void setCollectedBudget(String collectedBudget) { this.collectedBudget = collectedBudget; }

    public boolean getQualified() { return qualified; }
    public void setQualified(boolean qualified) { this.qualified = qualified; }

    public boolean getEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }

    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }

    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public long getTotalResponseTimeMs() { return totalResponseTimeMs; }
    public void setTotalResponseTimeMs(long totalResponseTimeMs) { this.totalResponseTimeMs = totalResponseTimeMs; }

    public int getResponseCount() { return responseCount; }
    public void setResponseCount(int responseCount) { this.responseCount = responseCount; }
}
