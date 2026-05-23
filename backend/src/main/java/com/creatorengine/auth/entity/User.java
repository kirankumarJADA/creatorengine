package com.creatorengine.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * User document — stored in Firestore at {@code users/{uid}}.
 *
 * <p>The document {@code id} is the same UID that Firebase Authentication
 * assigns when the user is created — this lets us pivot freely between
 * the two services without maintaining a separate mapping table.</p>
 *
 * <p>Passwords live in Firebase Auth, not here. There is intentionally
 * no {@code passwordHash} field — verifying credentials is delegated to
 * Firebase, which means we get bcrypt-equivalent hashing, password
 * reset emails, and future social login for free.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @DocumentId
    private String uid;

    private String email;
    private String name;
    private String avatarUrl;

    @Builder.Default
    private List<Role> roles = List.of(Role.USER);

    private boolean emailVerified;
    private boolean enabled;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}
