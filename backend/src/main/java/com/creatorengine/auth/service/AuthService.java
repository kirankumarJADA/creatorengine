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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthClient firebaseAuthClient;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties props;

    public AuthService(
            UserRepository userRepository,
            FirebaseAuth firebaseAuth,
            FirebaseAuthClient firebaseAuthClient,
            JwtTokenProvider tokenProvider,
            AppProperties props
    ) {
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        this.firebaseAuthClient = firebaseAuthClient;
        this.tokenProvider = tokenProvider;
        this.props = props;
    }

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

    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.email());

        String uid = firebaseAuthClient.verifyPassword(email, req.password());

        User user = userRepository.findById(uid).orElseGet(() -> {
            log.info("Re-hydrating missing Firestore profile for uid={}", uid);
            return userRepository.save(User.builder()
                    .uid(uid)
                    .email(email)
                    .name(email)
                    .roles(List.of(Role.USER))
                    .enabled(true)
                    .build());
        });

        if (!user.getEnabled()) {
            throw new UnauthorizedException("This account has been disabled.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.debug("Login OK for uid={}", uid);
        return buildAuthResponse(user);
    }

    public UserResponse getCurrentUser(String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        return UserResponse.from(user);
    }

    public void logout() {
        log.debug("Logout called (no-op for stateless JWT).");
    }

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