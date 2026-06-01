package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Instagram OAuth token endpoints.
 * Short-lived exchange returns access_token + user_id.
 * Long-lived exchange returns access_token + token_type + expires_in.
 */
public record MetaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type")   String tokenType,
        @JsonProperty("expires_in")   Long expiresIn,
        @JsonProperty("user_id")      Long userId
) {}
