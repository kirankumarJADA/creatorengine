package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.instagram.dto.ConnectResponse;
import com.creatorengine.instagram.dto.StatusResponse;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.InstagramOAuthService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The four OAuth + state endpoints.
 *
 * <p>Three of them ({@code /connect}, {@code /disconnect},
 * {@code /status}) require an authenticated CreatorEngine user.
 * Only {@code /callback} is public, since Meta calls it directly —
 * it authenticates via the signed {@code state} JWT instead.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
@Tag(name = "Instagram", description = "Instagram Business account connection")
public class InstagramController {

    private final InstagramOAuthService oauthService;
    private final InstagramAccountService accountService;

    @GetMapping("/connect")
    @Operation(summary = "Get the Meta authorization URL for the current user")
    public ResponseEntity<ApiResponse<ConnectResponse>> connect() {
        String uid = SecurityUtils.getCurrentUserId();
        String url = oauthService.buildAuthorizationUrl(uid);
        return ResponseEntity.ok(ApiResponse.ok(new ConnectResponse(url)));
    }

    @GetMapping("/callback")
    @Operation(summary = "OAuth callback — exchanges code for tokens and stores the account")
    public RedirectView callback(
            @RequestParam(value = "code",  required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {

        // User cancelled / Meta surfaced an error
        if (error != null) {
            log.info("OAuth callback returned error: {} ({})", error, errorDescription);
            return new RedirectView(oauthService.buildFailureRedirect(
                    errorDescription != null ? errorDescription : error));
        }

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return new RedirectView(oauthService.buildFailureRedirect(
                    "Missing OAuth code or state."));
        }

        try {
            oauthService.completeAuthorization(code, state);
            return new RedirectView(oauthService.buildSuccessRedirect());
        } catch (UnauthorizedException ex) {
            return new RedirectView(oauthService.buildFailureRedirect("Session expired."));
        } catch (Exception ex) {
            log.warn("OAuth callback failed: {}", ex.getMessage());
            return new RedirectView(oauthService.buildFailureRedirect(ex.getMessage()));
        }
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect the current user's Instagram account")
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        String uid = SecurityUtils.getCurrentUserId();
        accountService.disconnect(uid);
        return ResponseEntity.ok(ApiResponse.ok("Instagram account disconnected."));
    }

    @GetMapping("/status")
    @Operation(summary = "Connection status + safe profile fields for the current user")
    public ResponseEntity<ApiResponse<StatusResponse>> status() {
        String uid = SecurityUtils.getCurrentUserId();
        StatusResponse body = accountService.find(uid)
                .map(StatusResponse::from)
                .orElseGet(StatusResponse::notConnected);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
