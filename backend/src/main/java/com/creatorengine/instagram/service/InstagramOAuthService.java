package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaPagesResponse;
import com.creatorengine.instagram.dto.MetaTokenResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Orchestrates the Meta OAuth dance.
 *
 * <pre>
 *   User → /api/instagram/connect
 *       ← { authUrl }   (frontend redirects browser to this URL)
 *   Browser → facebook.com/dialog/oauth?…&state=&lt;signed UID&gt;
 *   Browser ← redirect to /api/instagram/callback?code=…&state=…
 *
 *   Backend in /callback:
 *     1. parseOAuthStateUserId(state) → uid           (rejects forged/expired state)
 *     2. exchangeCodeForToken(code)                   → short-lived user token
 *     3. exchangeForLongLivedToken(short)             → long-lived (~60d) user token
 *     4. listPages(longLived)                         → user's Pages
 *     5. fetchIgProfile(igId, pageAccessToken)        → handle, name, avatar
 *     6. save InstagramAccount                        → users/{uid}/instagram_account/profile
 *     7. 302 → frontend success page
 * </pre>
 *
 * <p>Errors at any step bubble up as {@link BadRequestException} so
 * the global exception handler returns a clean {@code ApiResponse}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramOAuthService {

    private static final String OAUTH_DIALOG_BASE =
            "https://www.facebook.com/%s/dialog/oauth";

    private final AppProperties props;
    private final JwtTokenProvider tokenProvider;
    private final InstagramApiClient apiClient;
    private final InstagramAccountService accountService;

    // ─── Step 1: build the Meta authorization URL ────────────
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
        String base  = OAUTH_DIALOG_BASE.formatted(meta.getGraphApiVersion());

        return UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("client_id",     meta.getAppId())
                .queryParam("redirect_uri",  meta.getRedirectUri())
                .queryParam("state",         state)
                .queryParam("scope",         meta.getScopes())
                .queryParam("response_type", "code")
                .build()
                .toUriString();
    }

    // ─── Step 2-7: handle Meta's callback ───────────────────
    /**
     * @return the UID that the OAuth flow was started for — caller uses
     *         it for the final redirect target.
     */
    public String completeAuthorization(String code, String state) {
        String uid = tokenProvider.parseOAuthStateUserId(state);
        if (uid == null) {
            throw new UnauthorizedException("OAuth state is invalid or expired.");
        }

        // 2. short-lived user token
        MetaTokenResponse shortToken = apiClient.exchangeCodeForToken(code);
        if (shortToken == null || shortToken.accessToken() == null) {
            throw new BadRequestException("Meta did not return an access token.");
        }

        // 3. long-lived user token
        MetaTokenResponse longToken = apiClient.exchangeForLongLivedToken(shortToken.accessToken());
        if (longToken == null || longToken.accessToken() == null) {
            throw new BadRequestException("Failed to upgrade to a long-lived token.");
        }

        // 4. find the user's first Page with an IG business account
        MetaPagesResponse pages = apiClient.listPages(longToken.accessToken());
        MetaPagesResponse.Page page = pickPageWithInstagram(pages);

        String pageId        = page.id();
        String pageToken     = page.accessToken();
        String igUserId      = page.instagramBusinessAccount().id();

        // 5. resolve IG profile (handle, name, avatar)
        MetaIgProfileResponse profile = apiClient.fetchIgProfile(igUserId, pageToken);

        // 6. persist
        Instant tokenExpiresAt = longToken.expiresIn() != null
                ? Instant.now().plusSeconds(longToken.expiresIn())
                : null;

        InstagramAccount account = InstagramAccount.builder()
                .instagramUserId(igUserId)
                .username(profile.username())
                .name(profile.name())
                .pageId(pageId)
                // We store the PAGE access token — IG Graph API calls need it.
                .accessToken(pageToken)
                .profilePictureUrl(profile.profilePictureUrl())
                .tokenExpiresAt(tokenExpiresAt)
                .connected(true)
                .build();
        accountService.save(uid, account);

        log.info("Connected Instagram account ig={} for uid={}", igUserId, uid);
        return uid;
    }

    // ─── Success-redirect URL builder ────────────────────────
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
            b.queryParam("message",
                    URLEncoder.encode(message, StandardCharsets.UTF_8));
        }
        return b.build().toUriString();
    }

    // ─── Helpers ─────────────────────────────────────────────
    private MetaPagesResponse.Page pickPageWithInstagram(MetaPagesResponse pages) {
        if (pages == null || pages.data() == null || pages.data().isEmpty()) {
            throw new BadRequestException(
                    "No Facebook Pages found for this user. "
                            + "Create a Page and link an Instagram Business account.");
        }
        return pages.data().stream()
                .filter(p -> p.instagramBusinessAccount() != null
                        && p.instagramBusinessAccount().id() != null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "None of your Pages have a linked Instagram Business account."));
    }
}
