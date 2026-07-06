package com.creatorengine.auth.service;

import com.creatorengine.auth.dto.AuthResponse;
import com.creatorengine.auth.dto.ChangePasswordRequest;
import com.creatorengine.auth.dto.ForgotPasswordRequest;
import com.creatorengine.auth.dto.GoogleAuthRequest;
import com.creatorengine.auth.dto.LoginRequest;
import com.creatorengine.auth.dto.SendOtpRequest;
import com.creatorengine.auth.dto.UpdateProfileRequest;
import com.creatorengine.auth.dto.UserResponse;
import com.creatorengine.auth.dto.VerifyOtpRequest;
import com.creatorengine.auth.entity.Role;
import com.creatorengine.auth.entity.User;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.ConflictException;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.security.JwtTokenProvider;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final OtpService otpService;
    private final ResendEmailService resendEmailService;

    public AuthService(
            UserRepository userRepository,
            FirebaseAuth firebaseAuth,
            FirebaseAuthClient firebaseAuthClient,
            JwtTokenProvider tokenProvider,
            AppProperties props,
            OtpService otpService,
            ResendEmailService resendEmailService
    ) {
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        this.firebaseAuthClient = firebaseAuthClient;
        this.tokenProvider = tokenProvider;
        this.props = props;
        this.otpService = otpService;
        this.resendEmailService = resendEmailService;
    }

    public void sendOtp(SendOtpRequest req) {
        String email = normalize(req.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        String otp = otpService.generate(email);
        resendEmailService.sendOtpEmail(email, otp);
        log.info("OTP sent for signup email={}", email);
    }

    public AuthResponse verifyOtpAndRegister(VerifyOtpRequest req) {
        String email = normalize(req.email());

        if (!otpService.verify(email, req.otp())) {
            throw new BadRequestException(
                    "Invalid or expired verification code. Please try again.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        UserRecord record;
        try {
            UserRecord.CreateRequest createReq = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setEmailVerified(true)
                    .setPassword(req.password())
                    .setDisplayName(req.name().trim())
                    .setDisabled(false);
            record = firebaseAuth.createUser(createReq);
        } catch (FirebaseAuthException ex) {
    log.error("GOOGLE_SIGNIN_DEBUG code={} message={}", ex.getAuthErrorCode(), ex.getMessage(), ex);
    throw new UnauthorizedException("Invalid Google token. Please try again.");
}
        User user = User.builder()
                .uid(record.getUid())
                .email(email)
                .name(req.name().trim())
                .roles(List.of(Role.USER))
                .emailVerified(true)
                .enabled(true)
                .lastLoginAt(Instant.now())
                .build();

        user = userRepository.save(user);
        log.info("Registered user uid={} email={}", user.getUid(), user.getEmail());
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

    public AuthResponse googleSignIn(GoogleAuthRequest req) {
        try {
            FirebaseToken decoded = firebaseAuth.verifyIdToken(req.idToken());

            String uid = decoded.getUid();
            String email = normalize(decoded.getEmail());
            String name = decoded.getName();
            if (name == null || name.isBlank()) name = email;

            final String finalName = name;

            User user = userRepository.findById(uid).orElseGet(() -> {
                User newUser = User.builder()
                        .uid(uid)
                        .email(email)
                        .name(finalName)
                        .roles(List.of(Role.USER))
                        .emailVerified(true)
                        .enabled(true)
                        .lastLoginAt(Instant.now())
                        .build();
                return userRepository.save(newUser);
            });

            if (!user.getEnabled()) {
                throw new UnauthorizedException("This account has been disabled.");
            }

            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("Google sign-in uid={} email={}", uid, email);
            return buildAuthResponse(user);

        } catch (FirebaseAuthException ex) {
            log.warn("Google sign-in failed: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid Google token. Please try again.");
        }
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || !tokenProvider.isValidRefreshToken(refreshToken)) {
            throw new UnauthorizedException(
                    "Invalid or expired refresh token. Please log in again.");
        }

        Claims claims = tokenProvider.parse(refreshToken);
        String uid = claims.getSubject();

        User user = userRepository.findById(uid)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists."));

        if (!user.getEnabled()) {
            throw new UnauthorizedException("This account has been disabled.");
        }

        log.debug("Refreshed tokens for uid={}", uid);
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

    public UserResponse updateProfile(String uid, UpdateProfileRequest req) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));

        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Name cannot be empty.");
        }

        user.setName(name);
        userRepository.save(user);

        try {
            firebaseAuth.updateUser(
                    new UserRecord.UpdateRequest(uid).setDisplayName(name));
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase displayName sync failed uid={}: {}", uid, ex.getMessage());
        }

        log.info("Updated profile name for uid={}", uid);
        return UserResponse.from(user);
    }

    public void changePassword(String uid, ChangePasswordRequest req) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));

        try {
            firebaseAuthClient.verifyPassword(user.getEmail(), req.currentPassword());
        } catch (Exception ex) {
            throw new UnauthorizedException("Current password is incorrect.");
        }

        try {
            firebaseAuth.updateUser(
                    new UserRecord.UpdateRequest(uid).setPassword(req.newPassword()));
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase password update failed uid={}: {}", uid, ex.getMessage());
            throw new BadRequestException("Could not update password. Please try again.");
        }

        log.info("Password changed for uid={}", uid);
    }

    public void sendPasswordResetEmail(ForgotPasswordRequest req) {
        String email = normalize(req.email());

        if (!userRepository.existsByEmail(email)) {
            log.debug("Password reset for non-existent email (silenced): {}", email);
            return;
        }

        try {
            ActionCodeSettings settings = ActionCodeSettings.builder()
                    .setUrl(props.getFrontendBaseUrl() + "/login")
                    .setHandleCodeInApp(false)
                    .build();

            String firebaseLink = firebaseAuth.generatePasswordResetLink(email, settings);
            String oobCode = extractQueryParam(firebaseLink, "oobCode");

            if (oobCode == null || oobCode.isBlank()) {
                log.warn("Could not extract oobCode from Firebase link for {}", email);
                return;
            }

            String apiKey = props.getFirebase().getWebApiKey();
            String customLink = props.getFrontendBaseUrl()
                    + "/auth/action?mode=resetPassword&oobCode="
                    + URLEncoder.encode(oobCode, StandardCharsets.UTF_8)
                    + "&apiKey="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            resendEmailService.sendPasswordResetEmail(email, customLink);
            log.info("Password reset email sent via Brevo for {}", email);
        } catch (Exception ex) {
            log.warn("Password reset failed for {}: {}", email, ex.getMessage());
        }
    }

    private static String extractQueryParam(String url, String paramName) {
        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null) return null;

            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx < 0) continue;
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                if (key.equals(paramName)) {
                    return URLDecoder.decode(value, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse query param '{}': {}", paramName, ex.getMessage());
        }
        return null;
    }

    private AuthResponse buildAuthResponse(User user) {
        String access = tokenProvider.generateAccessToken(
                user.getUid(), user.getEmail(), user.getRoles());
        String refresh = tokenProvider.generateRefreshToken(
                user.getUid(), user.getEmail());
        long expiresIn =
                props.getSecurity().getJwt().getAccessTokenExpirationMs() / 1000;

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