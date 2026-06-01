package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaTokenResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class InstagramApiClient {

    private static final Logger log = LoggerFactory.getLogger(InstagramApiClient.class);

    private static final String AUTH_BASE  = "https://api.instagram.com";
    private static final String GRAPH_BASE = "https://graph.instagram.com";

    private final AppProperties props;
    private final RestClient http = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .build();

    public InstagramApiClient(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (props.getMeta() == null || props.getMeta().getAppId() == null
                || props.getMeta().getAppId().isBlank()) {
            log.warn("META_APP_ID is not configured. Instagram OAuth will fail until it is set.");
        }
    }

    /** Step 1: exchange the authorization code for a short-lived IG user token (+ user_id). */
    public MetaTokenResponse exchangeCodeForToken(String code) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", props.getMeta().getAppId());
            form.add("client_secret", props.getMeta().getAppSecret());
            form.add("grant_type", "authorization_code");
            form.add("redirect_uri", props.getMeta().getRedirectUri());
            form.add("code", code);

            return http.post()
                    .uri(AUTH_BASE + "/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to exchange OAuth code", ex);
        }
    }

    /** Step 2: exchange the short-lived token for a 60-day long-lived token. */
    public MetaTokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        try {
            String url = GRAPH_BASE
                    + "/access_token"
                    + "?grant_type=ig_exchange_token"
                    + "&client_secret=" + URLEncoder.encode(props.getMeta().getAppSecret(), StandardCharsets.UTF_8)
                    + "&access_token=" + URLEncoder.encode(shortLivedToken, StandardCharsets.UTF_8);
            return http.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to upgrade to long-lived token", ex);
        }
    }

    /** Step 3: fetch the connected IG professional account's profile. */
    public MetaIgProfileResponse fetchIgProfile(String igUserToken) {
        try {
            String url = GRAPH_BASE
                    + "/me"
                    + "?fields=user_id,username,name,profile_picture_url"
                    + "&access_token=" + URLEncoder.encode(igUserToken, StandardCharsets.UTF_8);
            return http.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(MetaIgProfileResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to fetch Instagram profile", ex);
        }
    }

    public void refreshLongLivedTokenPlaceholder(String currentLongLivedToken) {
        log.debug("Token refresh placeholder - not yet implemented.");
    }

    private BadRequestException translate(String prefix, HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        log.warn("{}: status={}, body={}", prefix, ex.getStatusCode(), body);
        return new BadRequestException(prefix + ". Meta returned " + ex.getStatusCode() + ".");
    }
}