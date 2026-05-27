package com.creatorengine.instagram.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public class InstagramAccount {

    public static final String DOC_ID = "profile";

    private String instagramUserId;
    private String username;
    private String name;
    private String pageId;
    private String profilePictureUrl;

    @JsonIgnore
    private String accessToken;

    private boolean connected;
    private Instant connectedAt;
    private Instant lastSyncAt;
    private Instant tokenExpiresAt;

    public InstagramAccount() {
    }

    public InstagramAccount(
            String instagramUserId,
            String username,
            String name,
            String pageId,
            String profilePictureUrl,
            String accessToken,
            boolean connected,
            Instant connectedAt,
            Instant lastSyncAt,
            Instant tokenExpiresAt
    ) {
        this.instagramUserId = instagramUserId;
        this.username = username;
        this.name = name;
        this.pageId = pageId;
        this.profilePictureUrl = profilePictureUrl;
        this.accessToken = accessToken;
        this.connected = connected;
        this.connectedAt = connectedAt;
        this.lastSyncAt = lastSyncAt;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public static InstagramAccountBuilder builder() {
        return new InstagramAccountBuilder();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean getConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public static class InstagramAccountBuilder {
        private String instagramUserId;
        private String username;
        private String name;
        private String pageId;
        private String profilePictureUrl;
        private String accessToken;
        private boolean connected;
        private Instant connectedAt;
        private Instant lastSyncAt;
        private Instant tokenExpiresAt;

        public InstagramAccountBuilder instagramUserId(String instagramUserId) {
            this.instagramUserId = instagramUserId;
            return this;
        }

        public InstagramAccountBuilder username(String username) {
            this.username = username;
            return this;
        }

        public InstagramAccountBuilder name(String name) {
            this.name = name;
            return this;
        }

        public InstagramAccountBuilder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public InstagramAccountBuilder profilePictureUrl(String profilePictureUrl) {
            this.profilePictureUrl = profilePictureUrl;
            return this;
        }

        public InstagramAccountBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public InstagramAccountBuilder connected(boolean connected) {
            this.connected = connected;
            return this;
        }

        public InstagramAccountBuilder connectedAt(Instant connectedAt) {
            this.connectedAt = connectedAt;
            return this;
        }

        public InstagramAccountBuilder lastSyncAt(Instant lastSyncAt) {
            this.lastSyncAt = lastSyncAt;
            return this;
        }

        public InstagramAccountBuilder tokenExpiresAt(Instant tokenExpiresAt) {
            this.tokenExpiresAt = tokenExpiresAt;
            return this;
        }

        public InstagramAccount build() {
            return new InstagramAccount(
                    instagramUserId,
                    username,
                    name,
                    pageId,
                    profilePictureUrl,
                    accessToken,
                    connected,
                    connectedAt,
                    lastSyncAt,
                    tokenExpiresAt
            );
        }
    }
}