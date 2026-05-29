package com.creatorengine.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;
import java.util.List;

public class User {

    @DocumentId
    private String uid;

    private String email;
    private String name;
    private String avatarUrl;
    private List<Role> roles = List.of(Role.USER);
    private boolean emailVerified;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    public User() {
    }

    public User(
            String uid,
            String email,
            String name,
            String avatarUrl,
            List<Role> roles,
            boolean emailVerified,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt
    ) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
        this.emailVerified = emailVerified;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public static class UserBuilder {
        private String uid;
        private String email;
        private String name;
        private String avatarUrl;
        private List<Role> roles = List.of(Role.USER);
        private boolean emailVerified;
        private boolean enabled;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant lastLoginAt;

        public UserBuilder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserBuilder roles(List<Role> roles) {
            this.roles = roles;
            return this;
        }

        public UserBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserBuilder lastLoginAt(Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public User build() {
            return new User(
                    uid,
                    email,
                    name,
                    avatarUrl,
                    roles,
                    emailVerified,
                    enabled,
                    createdAt,
                    updatedAt,
                    lastLoginAt
            );
        }
    }
}