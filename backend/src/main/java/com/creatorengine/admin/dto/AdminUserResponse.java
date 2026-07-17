package com.creatorengine.admin.dto;

import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;
import com.creatorengine.plan.entity.Plan;

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
        Plan plan,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static AdminUserResponse from(User u, boolean igConnected, String igUsername, Plan plan) {
        return new AdminUserResponse(
                u.getUid(),
                u.getEmail(),
                u.getName(),
                u.getRoles(),
                u.getEmailVerified(),
                u.getEnabled(),
                igConnected,
                igUsername,
                plan,
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}