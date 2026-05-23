package com.creatorengine.instagram.dto;

import com.creatorengine.instagram.entity.ConnectionStatus;
import com.creatorengine.instagram.entity.InstagramAccount;
import lombok.Builder;

import java.time.Instant;

/**
 * Returned by {@code GET /api/instagram/status}.
 *
 * <p>The {@code accessToken} is deliberately absent — clients never
 * need to see it. Only safe profile fields are exposed.</p>
 */
@Builder
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
        if (!a.isConnected()) return ConnectionStatus.NOT_CONNECTED;
        Instant exp = a.getTokenExpiresAt();
        // Treat tokens expiring within an hour as already expired —
        // gives the user a chance to reconnect before things break.
        if (exp != null && exp.isBefore(Instant.now().plusSeconds(3600))) {
            return ConnectionStatus.EXPIRED;
        }
        return ConnectionStatus.CONNECTED;
    }
}
