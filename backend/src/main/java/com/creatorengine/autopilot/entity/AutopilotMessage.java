package com.creatorengine.autopilot.entity;

import java.util.Date;

/** One turn in an Autopilot conversation's memory. */
public class AutopilotMessage {

    private String role; // "user" or "assistant"
    private String content;
    private Date at;

    public AutopilotMessage() {}

    public AutopilotMessage(String role, String content, Date at) {
        this.role = role;
        this.content = content;
        this.at = at;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getAt() { return at; }
    public void setAt(Date at) { this.at = at; }
}
