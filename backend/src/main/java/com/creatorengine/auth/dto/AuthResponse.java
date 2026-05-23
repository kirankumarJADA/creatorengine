package com.creatorengine.auth.dto;

import lombok.Builder;

/**
 * Returned by /register and /login.
 */
@Builder
public record AuthResponse(
        UserResponse user,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn   // seconds until accessToken expires
) {}
