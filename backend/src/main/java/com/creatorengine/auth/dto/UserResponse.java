package com.creatorengine.auth.dto;

import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;

import java.time.Instant;
import java.util.List;

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
        return new UserResponse(
                u.getUid(),
                u.getEmail(),
                u.getName(),
                u.getAvatarUrl(),
                u.getRoles(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}