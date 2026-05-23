package com.creatorengine.auth.dto;

import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Public-facing user view — never includes credentials or internal flags.
 */
@Builder
public record UserResponse(
        String uid,
        String email,
        String name,
        String avatarUrl,
        List<Role> roles,
        boolean emailVerified,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserResponse from(User u) {
        return UserResponse.builder()
                .uid(u.getUid())
                .email(u.getEmail())
                .name(u.getName())
                .avatarUrl(u.getAvatarUrl())
                .roles(u.getRoles())
                .emailVerified(u.isEmailVerified())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }
}
