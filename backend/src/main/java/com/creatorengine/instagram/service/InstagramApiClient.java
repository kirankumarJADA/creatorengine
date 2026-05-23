package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaPagesResponse;
import com.creatorengine.instagram.dto.MetaTokenResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around the slice of Meta's Graph API we use today:
 * code↔token exchange, page lookup, and IG profile fetch.
 *
 * <p>One {@link RestClient} is lazily built per process — it's
 * thread-safe and keeps its own connection pool. We don't bake the
 * graph URL into the constructor because we want any
 * application.yml override (e.g. for testing) to take effect.</p>
 *
 * <p>Errors from Meta are translated into {@link BadRequestException}
 * with the Meta-supplied message so the user sees something useful
 * (e.g. "User has no Instagram Business account").</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramApiClient {

    private final AppProperties props;
    private volatile RestClient client;

    @PostConstruct
    void init() {
        if (props.getMeta() == null || props.getMeta().getAppId() == null
                || props.getMeta().getAppId().isBlank()) {
            log.warn("⚠️  META_APP_ID is not configured. The Instagram OAuth flow "
                    + "will fail at the first Meta call until you set it.");
        }
    }

    private RestClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = RestClient.builder()
                            .baseUrl(graphBase())
                            .defaultHeader("Accept", "application/json")
                            .build();
                }
            }
        }
        return client;
    }

    private String graphBase() {
        return "https://graph.facebook.com/" + props.getMeta().getGraphApiVersion();
    }

    // ─── Token exchanges ─────────────────────────────────────

    /** Exchange the OAuth code for a short-lived user access token. */
    public MetaTokenResponse exchangeCodeForToken(String code) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/oauth/access_token")
                            .queryParam("client_id",     props.getMeta().getAppId())
                            .queryParam("client_secret", props.getMeta().getAppSecret())
                            .queryParam("redirect_uri",  props.getMeta().getRedirectUri())
                            .queryParam("code",          code)
                            .build())
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to exchange OAuth code", ex);
        }
    }

    /**
     * Upgrade a short-lived user token to a ~60-day long-lived one.
     * Cheap, idempotent, safe to retry.
     */
    public MetaTokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/oauth/access_token")
                            .queryParam("grant_type",        "fb_exchange_token")
                            .queryParam("client_id",         props.getMeta().getAppId())
                            .queryParam("client_secret",     props.getMeta().getAppSecret())
                            .queryParam("fb_exchange_token", shortLivedToken)
                            .build())
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to upgrade to long-lived token", ex);
        }
    }

    // ─── Page + IG profile lookups ──────────────────────────

    /** List the Pages the user manages, with their linked IG business accounts. */
    public MetaPagesResponse listPages(String userAccessToken) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/me/accounts")
                            .queryParam("fields",
                                    "id,name,access_token,instagram_business_account{id}")
                            .queryParam("access_token", userAccessToken)
                            .build())
                    .retrieve()
                    .body(MetaPagesResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to fetch user's pages", ex);
        }
    }

    /** Fetch IG profile fields for a given IG business account id. */
    public MetaIgProfileResponse fetchIgProfile(String igUserId, String pageAccessToken) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/" + igUserId)
                            .queryParam("fields", "id,username,name,profile_picture_url")
                            .queryParam("access_token", pageAccessToken)
                            .build())
                    .retrieve()
                    .body(MetaIgProfileResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to fetch Instagram profile", ex);
        }
    }

    // ─── Future: token refresh ──────────────────────────────
    /**
     * Placeholder for the scheduled refresh job.
     * Long-lived user tokens can be re-extended by calling the
     * {@code fb_exchange_token} endpoint with the existing long-lived
     * token — extending it by another 60 days. Wire this into a
     * {@code @Scheduled} task that runs daily and refreshes any
     * accounts whose tokens expire in less than 7 days.
     *
     * Intentionally unimplemented for the MVP per spec.
     */
    public void refreshLongLivedTokenPlaceholder(String currentLongLivedToken) {
        // TODO: scheduled refresh — call exchangeForLongLivedToken(currentLongLivedToken)
        //       and persist the new accessToken + tokenExpiresAt.
        log.debug("Token refresh placeholder — not yet implemented.");
    }

    // ─── Error translation ──────────────────────────────────
    private BadRequestException translate(String prefix, HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        log.warn("{}: status={}, body={}", prefix, ex.getStatusCode(), body);
        // Body is usually:  { "error": { "message": "...", "type": "...", "code": ... } }
        // We don't deserialise it here to keep this method dependency-light — the
        // raw body is informative enough for ops, and the user-facing message is
        // generic on purpose to avoid leaking IDs.
        return new BadRequestException(
                prefix + ". Meta returned " + ex.getStatusCode() + ".");
    }
}
