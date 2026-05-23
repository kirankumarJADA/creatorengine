package com.creatorengine.instagram.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The Instagram Business account connected to a CreatorEngine user.
 *
 * <p>Persisted at {@code users/{uid}/instagram_account/profile} —
 * one document per user, fixed id "profile" so callers don't need
 * to know the doc id to find it.</p>
 *
 * <p><b>SECURITY:</b> {@code accessToken} is stored in plaintext for
 * the MVP. Before going to production, encrypt it at rest using a
 * KMS-backed key (Google Cloud KMS, AWS KMS, etc.) — the encryption
 * boundary should be drawn here. The token is marked {@code @JsonIgnore}
 * so it can never leak through a controller response.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramAccount {

    /** Always the literal string "profile" for the single account doc. */
    public static final String DOC_ID = "profile";

    private String instagramUserId;   // IG business account id
    private String username;          // @handle
    private String name;              // display name
    private String pageId;            // linked Facebook Page id
    private String profilePictureUrl; // optional cached URL

    /** Page access token used to call Graph API on the user's behalf. */
    @JsonIgnore
    private String accessToken;

    private boolean connected;
    private Instant connectedAt;
    private Instant lastSyncAt;

    /** When the access token expires. Null when long-lived without explicit expiry. */
    private Instant tokenExpiresAt;
}
