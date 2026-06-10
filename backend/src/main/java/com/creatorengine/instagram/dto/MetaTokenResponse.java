package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response shape for {@code /oauth/access_token} and the long-lived /
 * refresh token endpoints.
 *
 * <p>Meta returns {@code access_token}, {@code token_type}, an optional
 * {@code expires_in} (seconds), and — on the initial code exchange — a
 * {@code user_id}. The long-lived and refresh responses omit
 * {@code user_id}, so it's simply null there.</p>
 */
public record MetaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type")   String tokenType,
        @JsonProperty("expires_in")   Long expiresIn,
        @JsonProperty("user_id")      Long userId
) {}