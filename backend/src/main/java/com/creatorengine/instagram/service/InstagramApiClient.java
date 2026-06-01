package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.instagram.dto.MetaIgProfileResponse;
import com.creatorengine.instagram.dto.MetaPagesResponse;
import com.creatorengine.instagram.dto.MetaTokenResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
public class InstagramApiClient {

    private static final Logger log = LoggerFactory.getLogger(InstagramApiClient.class);

    private final AppProperties props;
    private volatile RestClient client;

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

    public MetaTokenResponse exchangeCodeForToken(String code) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/oauth/access_token")
                            .queryParam("client_id", props.getMeta().getAppId())
                            .queryParam("client_secret", props.getMeta().getAppSecret())
                            .queryParam("redirect_uri", props.getMeta().getRedirectUri())
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to exchange OAuth code", ex);
        }
    }

    public MetaTokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        try {
            return client().get()
                    .uri(uri -> uri
                            .path("/oauth/access_token")
                            .queryParam("grant_type", "fb_exchange_token")
                            .queryParam("client_id", props.getMeta().getAppId())
                            .queryParam("client_secret", props.getMeta().getAppSecret())
                            .queryParam("fb_exchange_token", shortLivedToken)
                            .build())
                    .retrieve()
                    .body(MetaTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to upgrade to long-lived token", ex);
        }
    }

   public MetaPagesResponse listPages(String userAccessToken) {
    try {
        return client().get()
                .uri(uri -> uri
                        .path("/me/accounts")
                        .queryParam("fields",
                                "id,name,access_token,instagram_business_account%7Bid%7D")
                        .queryParam("access_token", userAccessToken)
                        .build())
                .retrieve()
                .body(MetaPagesResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translate("Failed to fetch user's pages", ex);
        }
    }

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

    public void refreshLongLivedTokenPlaceholder(String currentLongLivedToken) {
        log.debug("Token refresh placeholder - not yet implemented.");
    }

    private BadRequestException translate(String prefix, HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        log.warn("{}: status={}, body={}", prefix, ex.getStatusCode(), body);
        return new BadRequestException(
                prefix + ". Meta returned " + ex.getStatusCode() + ".");
    }
}