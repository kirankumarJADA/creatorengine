package com.creatorengine.admin.dto;

import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;

import java.time.Instant;
import java.util.List;

public record AdminUserResponse(
        String uid,
        String email,
        String name,
        List<Role> roles,
        boolean emailVerified,
        boolean enabled,
        boolean instagramConnected,
        String instagramUsername,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static AdminUserResponse from(User u, boolean igConnected, String igUsername) {
        return new AdminUserResponse(
                u.getUid(),
                u.getEmail(),
                u.getName(),
                u.getRoles(),
                u.getEmailVerified(),
                u.getEnabled(),
                igConnected,
                igUsername,
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}