package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.instagram.dto.ConnectResponse;
import com.creatorengine.instagram.dto.MetaMediaResponse;
import com.creatorengine.instagram.dto.StatusResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.InstagramApiClient;
import com.creatorengine.instagram.service.InstagramOAuthService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequestMapping("/api/instagram")
@Tag(name = "Instagram", description = "Instagram Business account connection")
public class InstagramController {

    private static final Logger log = LoggerFactory.getLogger(InstagramController.class);

    private final InstagramOAuthService oauthService;
    private final InstagramAccountService accountService;
    private final InstagramApiClient apiClient;

    public InstagramController(
            InstagramOAuthService oauthService,
            InstagramAccountService accountService,
            InstagramApiClient apiClient
    ) {
        this.oauthService = oauthService;
        this.accountService = accountService;
        this.apiClient = apiClient;
    }

    @GetMapping("/connect")
    @Operation(summary = "Get the Meta authorization URL for the current user")
    public ResponseEntity<ApiResponse<ConnectResponse>> connect() {
        String uid = SecurityUtils.getCurrentUserId();
        String url = oauthService.buildAuthorizationUrl(uid);
        return ResponseEntity.ok(ApiResponse.ok(new ConnectResponse(url)));
    }

    @GetMapping("/callback")
    @Operation(summary = "OAuth callback - exchanges code for tokens and stores the account")
    public RedirectView callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription
    ) {
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

    /**
     * Disconnect a specific Instagram account by its Instagram user ID.
     * If no igAccountId is provided, disconnects ALL accounts (legacy behavior).
     */
    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect an Instagram account")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @RequestParam(value = "igAccountId", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        if (igAccountId != null && !igAccountId.isBlank()) {
            accountService.disconnect(uid, igAccountId);
        } else {
            accountService.disconnectAll(uid);
        }
        return ResponseEntity.ok(ApiResponse.ok("Instagram account disconnected."));
    }

    @GetMapping("/status")
    @Operation(summary = "Connection status + safe profile fields for the current user")
    public ResponseEntity<ApiResponse<StatusResponse>> status() {
        String uid = SecurityUtils.getCurrentUserId();
        StatusResponse body = accountService.find(uid)
                .filter(acc -> !accountService.isTokenRevoked(acc.getAccessToken()))
                .map(StatusResponse::from)
                .orElseGet(StatusResponse::notConnected);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    /**
     * List ALL connected Instagram accounts for the current user.
     */
    @GetMapping("/accounts")
    @Operation(summary = "List all connected Instagram accounts for the current user")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> accounts() {
        String uid = SecurityUtils.getCurrentUserId();
        List<StatusResponse> accounts = accountService.findAll(uid).stream()
                .map(StatusResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @GetMapping("/media")
    @Operation(summary = "List the connected account's recent posts for the automation post-picker")
    public ResponseEntity<ApiResponse<List<MetaMediaResponse.MediaItem>>> media(
            @RequestParam(value = "igAccountId", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();

        InstagramAccount account;
        if (igAccountId != null && !igAccountId.isBlank()) {
            account = accountService.findByIgId(uid, igAccountId)
                    .orElseThrow(() -> new BadRequestException("Instagram account not found."));
        } else {
            account = accountService.find(uid)
                    .orElseThrow(() -> new BadRequestException("No Instagram account connected."));
        }

        MetaMediaResponse media = apiClient.fetchMedia(account.getAccessToken());
        List<MetaMediaResponse.MediaItem> items =
                (media != null && media.data() != null) ? media.data() : List.of();

        return ResponseEntity.ok(ApiResponse.ok(items));
    }
}