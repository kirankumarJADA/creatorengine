package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaPagesResponse;
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
            "https://www.facebook.com/%s/dialog/oauth";

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
        String base = OAUTH_DIALOG_BASE.formatted(meta.getGraphApiVersion());

        return UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("client_id", meta.getAppId())
                .queryParam("redirect_uri", meta.getRedirectUri())
                .queryParam("state", state)
                .queryParam("scope", meta.getScopes())
                .queryParam("response_type", "code")
                .build()
                .toUriString();
    }

    public String completeAuthorization(String code, String state) {
        String uid = tokenProvider.parseOAuthStateUserId(state);

        if (uid == null) {
            throw new UnauthorizedException("OAuth state is invalid or expired.");
        }

        MetaTokenResponse shortToken = apiClient.exchangeCodeForToken(code);
        if (shortToken == null || shortToken.accessToken() == null) {
            throw new BadRequestException("Meta did not return an access token.");
        }

        MetaTokenResponse longToken = apiClient.exchangeForLongLivedToken(shortToken.accessToken());
        if (longToken == null || longToken.accessToken() == null) {
            throw new BadRequestException("Failed to upgrade to a long-lived token.");
        }

        MetaPagesResponse pages = apiClient.listPages(longToken.accessToken());
        MetaPagesResponse.Page page = pickPageWithInstagram(pages);

        String pageId = page.id();
        String pageToken = page.accessToken();
        String igUserId = page.instagramBusinessAccount().id();

        MetaIgProfileResponse profile = apiClient.fetchIgProfile(igUserId, pageToken);

        Instant tokenExpiresAt = longToken.expiresIn() != null
                ? Instant.now().plusSeconds(longToken.expiresIn())
                : null;

        InstagramAccount account = InstagramAccount.builder()
                .instagramUserId(igUserId)
                .username(profile.username())
                .name(profile.name())
                .pageId(pageId)
                .accessToken(pageToken)
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

    private MetaPagesResponse.Page pickPageWithInstagram(MetaPagesResponse pages) {
        if (pages == null || pages.data() == null || pages.data().isEmpty()) {
            throw new BadRequestException(
                    "No Facebook Pages found for this user. Create a Page and link an Instagram Business account.");
        }

        return pages.data().stream()
                .filter(p -> p.instagramBusinessAccount() != null
                        && p.instagramBusinessAccount().id() != null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "None of your Pages have a linked Instagram Business account."));
    }
}