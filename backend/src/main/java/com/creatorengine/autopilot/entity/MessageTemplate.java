package com.creatorengine.autopilot.entity;

/**
 * A named, pre-written canned message the creator authors up front (lead
 * magnet link, coupon code, booking link, etc.). AI Autopilot may send one
 * of these verbatim instead of free-form text when "Send predefined DM
 * templates" is an allowed action — keeps sensitive offers/links under the
 * creator's exact wording rather than left to the model to phrase.
 */
public class MessageTemplate {

    private String id;
    private String label;       // shown to the AI + owner, e.g. "Free Guide"
    private String description; // short hint for the AI on when to use it
    private String message;     // the exact text sent to the customer

    public MessageTemplate() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
