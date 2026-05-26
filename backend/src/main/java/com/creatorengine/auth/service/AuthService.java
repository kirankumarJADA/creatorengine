package com.creatorengine.auth.service;

import com.creatorengine.auth.dto.AuthResponse;
import com.creatorengine.auth.dto.ForgotPasswordRequest;
import com.creatorengine.auth.dto.LoginRequest;
import com.creatorengine.auth.dto.RegisterRequest;
import com.creatorengine.auth.dto.UserResponse;
import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.ConflictException;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.security.JwtTokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Owns the full credential lifecycle.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li><b>Register</b> — create the user in Firebase Auth (which hashes
 *       the password using scrypt) and mirror a profile document in
 *       Firestore. Returns a freshly-minted JWT pair.</li>
 *   <li><b>Login</b> — verify credentials via Firebase REST, refresh the
 *       Firestore profile, and issue JWTs.</li>
 *   <li><b>Forgot password</b> — delegate to Firebase, which knows how to
 *       generate (and optionally email) a reset link.</li>
 *   <li><b>Me</b> — return the current user's profile.</li>
 * </ul>
 *
 * <p>We never store passwords ourselves — Firebase is the source of
 * truth for credentials, which also gives us social-login, MFA, and
 * password reset for free down the line.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthClient firebaseAuthClient;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties props;

    // ─── Register ────────────────────────────────────────────────
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        UserRecord record;
        try {
            UserRecord.CreateRequest createReq = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setEmailVerified(false)
                    .setPassword(req.password())
                    .setDisplayName(req.name().trim())
                    .setDisabled(false);
            record = firebaseAuth.createUser(createReq);
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase createUser failed: code={}, msg={}",
                    ex.getAuthErrorCode(), ex.getMessage());
            // The most common case is "email-already-exists" — but we already
            // checked Firestore above, so this would mean an orphan Firebase
            // record. Surface it as a conflict either way.
            throw new ConflictException(
                    "Unable to create account. The email may already be in use.");
        }

        User user = User.builder()
                .uid(record.getUid())
                .email(email)
                .name(req.name().trim())
                .roles(List.of(Role.USER))
                .emailVerified(false)
                .enabled(true)
                .lastLoginAt(Instant.now())
                .build();
        user = userRepository.save(user);

        log.info("Registered user uid={}, email={}", user.getUid(), user.getEmail());
        return buildAuthResponse(user);
    }

    // ─── Login ───────────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.email());

        // Step 1 — Firebase Auth verifies the password
        String uid = firebaseAuthClient.verifyPassword(email, req.password());

        // Step 2 — Load the matching profile from Firestore
        User user = userRepository.findById(uid).orElseGet(() -> {
            // Edge case: Firebase has the user but Firestore doesn't (e.g.
            // a manual creation in the Firebase console). Re-hydrate the
            // profile so the rest of the system sees a complete record.
            log.info("Re-hydrating missing Firestore profile for uid={}", uid);
            return userRepository.save(User.builder()
                    .uid(uid)
                    .email(email)
                    .name(email)
                    .roles(List.of(Role.USER))
                    .enabled(true)
                    .build());
        });

        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.debug("Login OK for uid={}", uid);
        return buildAuthResponse(user);
    }

    // ─── Me ──────────────────────────────────────────────────────
    public UserResponse getCurrentUser(String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        return UserResponse.from(user);
    }

    // ─── Logout ──────────────────────────────────────────────────
    public void logout() {
        // Stateless JWT — logout is a client concern (discard tokens).
        // If we later add a token blacklist or revoked-jti list, it'd live here.
        log.debug("Logout called (no-op for stateless JWT).");
    }

    // ─── Forgot password ─────────────────────────────────────────
    public void sendPasswordResetEmail(ForgotPasswordRequest req) {
        String email = normalize(req.email());

        if (!userRepository.existsByEmail(email)) {
            log.debug("Password reset requested for non-existent email (silenced): {}", email);
            return;
        }

        try {
            firebaseAuthClient.sendPasswordResetEmail(
                    email, props.getFirebase().getPasswordResetRedirectUrl());
            log.info("Password reset email triggered for {}", email);
        } catch (Exception ex) {
            log.warn("Password reset send failed for {}: {}", email, ex.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        String access = tokenProvider.generateAccessToken(
                user.getUid(), user.getEmail(), user.getRoles());
        String refresh = tokenProvider.generateRefreshToken(
                user.getUid(), user.getEmail());
        long expiresIn = props.getSecurity().getJwt().getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .user(UserResponse.from(user))
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
