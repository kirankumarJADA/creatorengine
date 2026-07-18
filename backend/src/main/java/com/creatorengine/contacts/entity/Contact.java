package com.creatorengine.contacts.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Contact {

    @DocumentId
    private String id;

    private String instagramUserId;
    private String username;
    private String email;
    private String source;
    private String lastMessage;
    private long totalTriggers;
    private Instant createdAt;
    private Instant updatedAt;

    // ─── AI Autopilot (#15) enrichment fields ─────────────────────
    private String name;
    private String phone;
    private String preferences;
    private String budget;
    private List<String> tags = new ArrayList<>();
    private boolean qualified;

    public Contact() {
    }

    public Contact(
            String id,
            String instagramUserId,
            String username,
            String email,
            String source,
            String lastMessage,
            long totalTriggers,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.instagramUserId = instagramUserId;
        this.username = username;
        this.email = email;
        this.source = source;
        this.lastMessage = lastMessage;
        this.totalTriggers = totalTriggers;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContactBuilder builder() {
        return new ContactBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstagramUserId() {
        return instagramUserId;
    }

    public void setInstagramUserId(String instagramUserId) {
        this.instagramUserId = instagramUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTotalTriggers() {
        return totalTriggers;
    }

    public void setTotalTriggers(long totalTriggers) {
        this.totalTriggers = totalTriggers;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public boolean getQualified() {
        return qualified;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public static class ContactBuilder {
        private String id;
        private String instagramUserId;
        private String username;
        private String email;
        private String source;
        private String lastMessage;
        private long totalTriggers;
        private Instant createdAt;
        private Instant updatedAt;

        public ContactBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ContactBuilder instagramUserId(String instagramUserId) {
            this.instagramUserId = instagramUserId;
            return this;
        }

        public ContactBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ContactBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ContactBuilder source(String source) {
            this.source = source;
            return this;
        }

        public ContactBuilder lastMessage(String lastMessage) {
            this.lastMessage = lastMessage;
            return this;
        }

        public ContactBuilder totalTriggers(long totalTriggers) {
            this.totalTriggers = totalTriggers;
            return this;
        }

        public ContactBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ContactBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Contact build() {
            return new Contact(
                    id,
                    instagramUserId,
                    username,
                    email,
                    source,
                    lastMessage,
                    totalTriggers,
                    createdAt,
                    updatedAt
            );
        }
    }
}
