package com.creatorengine.auth.controller;

import com.creatorengine.auth.dto.AuthResponse;
import com.creatorengine.auth.dto.ChangePasswordRequest;
import com.creatorengine.auth.dto.ForgotPasswordRequest;
import com.creatorengine.auth.dto.LoginRequest;
import com.creatorengine.auth.dto.RefreshRequest;
import com.creatorengine.auth.dto.SendOtpRequest;
import com.creatorengine.auth.dto.UpdateProfileRequest;
import com.creatorengine.auth.dto.UserResponse;
import com.creatorengine.auth.dto.VerifyOtpRequest;
import com.creatorengine.auth.service.AuthService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Step 1 of signup — send OTP to email */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Verification code sent. Check your email."));
    }

    /** Step 2 of signup — verify OTP and create account */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req) {
        AuthResponse resp = authService.verifyOtpAndRegister(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created.", resp));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Logged in.",
                authService.login(req)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed.",
                authService.refresh(req.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.ok("Logged out."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.getCurrentUser(SecurityUtils.getCurrentUserId())));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated.",
                authService.updateProfile(
                        SecurityUtils.getCurrentUserId(), req)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Password updated."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.sendPasswordResetEmail(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account exists for that email, a reset link has been sent."));
    }
}