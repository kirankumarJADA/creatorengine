package com.creatorengine.instagram.dto;

import com.creatorengine.instagram.entity.ConnectionStatus;
import com.creatorengine.instagram.entity.InstagramAccount;

import java.time.Instant;

public record StatusResponse(
        ConnectionStatus status,
        String instagramUserId,
        String username,
        String name,
        String pageId,
        String profilePictureUrl,
        Instant connectedAt,
        Instant lastSyncAt,
        Instant tokenExpiresAt
) {
    public static StatusResponseBuilder builder() {
        return new StatusResponseBuilder();
    }

    public static StatusResponse notConnected() {
        return StatusResponse.builder()
                .status(ConnectionStatus.NOT_CONNECTED)
                .build();
    }

    public static StatusResponse from(InstagramAccount a) {
        return StatusResponse.builder()
                .status(deriveStatus(a))
                .instagramUserId(a.getInstagramUserId())
                .username(a.getUsername())
                .name(a.getName())
                .pageId(a.getPageId())
                .profilePictureUrl(a.getProfilePictureUrl())
                .connectedAt(a.getConnectedAt())
                .lastSyncAt(a.getLastSyncAt())
                .tokenExpiresAt(a.getTokenExpiresAt())
                .build();
    }

    private static ConnectionStatus deriveStatus(InstagramAccount a) {
        if (!a.getConnected()) {
            return ConnectionStatus.NOT_CONNECTED;
        }

        Instant exp = a.getTokenExpiresAt();

        if (exp != null && exp.isBefore(Instant.now().plusSeconds(3600))) {
            return ConnectionStatus.EXPIRED;
        }

        return ConnectionStatus.CONNECTED;
    }

    public static class StatusResponseBuilder {
        private ConnectionStatus status;
        private String instagramUserId;
        private String username;
        private String name;
        private String pageId;
        private String profilePictureUrl;
        private Instant connectedAt;
        private Instant lastSyncAt;
        private Instant tokenExpiresAt;

        public StatusResponseBuilder status(ConnectionStatus status) {
            this.status = status;
            return this;
        }

        public StatusResponseBuilder instagramUserId(String instagramUserId) {
            this.instagramUserId = instagramUserId;
            return this;
        }

        public StatusResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public StatusResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StatusResponseBuilder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public StatusResponseBuilder profilePictureUrl(String profilePictureUrl) {
            this.profilePictureUrl = profilePictureUrl;
            return this;
        }

        public StatusResponseBuilder connectedAt(Instant connectedAt) {
            this.connectedAt = connectedAt;
            return this;
        }

        public StatusResponseBuilder lastSyncAt(Instant lastSyncAt) {
            this.lastSyncAt = lastSyncAt;
            return this;
        }

        public StatusResponseBuilder tokenExpiresAt(Instant tokenExpiresAt) {
            this.tokenExpiresAt = tokenExpiresAt;
            return this;
        }

        public StatusResponse build() {
            return new StatusResponse(
                    status,
                    instagramUserId,
                    username,
                    name,
                    pageId,
                    profilePictureUrl,
                    connectedAt,
                    lastSyncAt,
                    tokenExpiresAt
            );
        }
    }
}