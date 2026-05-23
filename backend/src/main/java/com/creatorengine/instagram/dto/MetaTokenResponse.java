package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of the response from {@code /oauth/access_token}.
 *
 * <p>Meta returns {@code access_token}, {@code token_type}, and an
 * optional {@code expires_in} (seconds, omitted for some long-lived
 * tokens). We map those into camelCase via {@code @JsonProperty}.</p>
 */
public record MetaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type")   String tokenType,
        @JsonProperty("expires_in")   Long expiresIn
) {}
