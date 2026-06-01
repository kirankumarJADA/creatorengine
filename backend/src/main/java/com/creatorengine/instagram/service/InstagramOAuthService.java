package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaTokenResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class InstagramOAuthService {

    private static final Logger log = LoggerFactory.getLogger(InstagramOAuthService.class);

    private static final String OAUTH_DIALOG_BASE =
            "https://www.instagram.com/oauth/authorize";

    private final AppProperties props;
    private final JwtTokenProvider tokenProvider;
    private final InstagramApiClient apiClient;
    private final InstagramAccountService accountService;

    public InstagramOAuthService(
            AppProperties props,
            JwtTokenProvider tokenProvider,
            InstagramApiClient apiClient,
            InstagramAccountService accountService
    ) {
        this.props = props;
        this.tokenProvider = tokenProvider;
        this.apiClient = apiClient;
        this.accountService = accountService;
    }

    public String buildAuthorizationUrl(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new UnauthorizedException("Authenticated user required.");
        }

        AppProperties.Meta meta = props.getMeta();

        if (meta.getAppId() == null || meta.getAppId().isBlank()) {
            throw new BadRequestException(
                    "Instagram integration not configured (META_APP_ID missing).");
        }

        String state = tokenProvider.generateOAuthStateToken(uid);

      return UriComponentsBuilder.fromHttpUrl(OAUTH_DIALOG_BASE)
                .queryParam("force_reauth", "true")
                .queryParam("client_id", meta.getAppId())
                .queryParam("redirect_uri", meta.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", meta.getScopes())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public String completeAuthorization(String code, String state) {
        String uid = tokenProvider.parseOAuthStateUserId(state);

        if (uid == null) {
            throw new UnauthorizedException("OAuth state is invalid or expired.");
        }

        // 1. Code -> short-lived IG user token (response includes user_id)
        MetaTokenResponse shortToken = apiClient.exchangeCodeForToken(code);
        if (shortToken == null || shortToken.accessToken() == null) {
            throw new BadRequestException("Meta did not return an access token.");
        }

        // 2. Short-lived -> long-lived (60 days)
        MetaTokenResponse longToken = apiClient.exchangeForLongLivedToken(shortToken.accessToken());
        if (longToken == null || longToken.accessToken() == null) {
            throw new BadRequestException("Failed to upgrade to a long-lived token.");
        }

        String igToken = longToken.accessToken();

        // 3. Fetch the IG professional account profile
        MetaIgProfileResponse profile = apiClient.fetchIgProfile(igToken);

        String igUserId = profile.userId() != null
                ? profile.userId()
                : (shortToken.userId() != null ? String.valueOf(shortToken.userId()) : null);

        if (igUserId == null) {
            throw new BadRequestException("Could not determine Instagram user id.");
        }

        Instant tokenExpiresAt = longToken.expiresIn() != null
                ? Instant.now().plusSeconds(longToken.expiresIn())
                : null;

        InstagramAccount account = InstagramAccount.builder()
                .instagramUserId(igUserId)
                .username(profile.username())
                .name(profile.name())
                .accessToken(igToken)
                .profilePictureUrl(profile.profilePictureUrl())
                .tokenExpiresAt(tokenExpiresAt)
                .connected(true)
                .build();

        accountService.save(uid, account);

        log.info("Connected Instagram account ig={} for uid={}", igUserId, uid);
        return uid;
    }

    public String buildSuccessRedirect() {
        return appendStatus(props.getMeta().getSuccessRedirectUri(), "success", null);
    }

    public String buildFailureRedirect(String reason) {
        return appendStatus(props.getMeta().getSuccessRedirectUri(), "error", reason);
    }

    private static String appendStatus(String base, String status, String message) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(base)
                .queryParam("status", status);

        if (message != null && !message.isBlank()) {
            b.queryParam("message", URLEncoder.encode(message, StandardCharsets.UTF_8));
        }

        return b.build().toUriString();
    }
}