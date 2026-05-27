package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MetaMessagingService {

    private static final Logger log = LoggerFactory.getLogger(MetaMessagingService.class);

    private final AppProperties props;
    private volatile RestClient client;

    public MetaMessagingService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (props.getMeta() == null || props.getMeta().getAppId() == null) {
            log.warn("Meta integration not fully configured; DMs will fail at send time.");
        }
    }

    private RestClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = RestClient.builder()
                            .baseUrl("https://graph.facebook.com/" + props.getMeta().getGraphApiVersion())
                            .defaultHeader("Content-Type", "application/json")
                            .defaultHeader("Accept", "application/json")
                            .build();
                }
            }
        }

        return client;
    }

    public SendResult sendText(Recipient recipient, String text, AccessTokenContext ctx) {
        if (recipient == null || text == null || text.isBlank()) {
            return SendResult.failure("Empty recipient or message.", 0);
        }

        if (ctx == null
                || ctx.instagramBusinessAccountId() == null
                || ctx.pageAccessToken() == null) {
            return SendResult.failure("Instagram account not connected.", 0);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipient", recipient.toJsonShape());
        body.put("message", Map.of("text", text));

        return post(ctx.instagramBusinessAccountId(), ctx.pageAccessToken(), body);
    }

    private SendResult post(String igAccountId, String pageAccessToken, Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri(uri -> uri
                            .path("/" + igAccountId + "/messages")
                            .queryParam("access_token", pageAccessToken)
                            .build())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null
                    ? String.valueOf(response.getOrDefault("message_id", ""))
                    : null;

            log.info("DM sent ig={} message_id={}", igAccountId, messageId);
            return SendResult.success(messageId);
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("DM send failed status={} body={}", ex.getStatusCode(), responseBody);
            return SendResult.failure(
                    "Meta returned " + ex.getStatusCode() + ": " + responseBody,
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            log.warn("DM send failed unexpectedly: {}", ex.getMessage());
            return SendResult.failure(ex.getMessage(), 0);
        }
    }

    public sealed interface Recipient permits ByUserId, ByCommentId {
        Map<String, Object> toJsonShape();
    }

    public record ByUserId(String userId) implements Recipient {
        @Override
        public Map<String, Object> toJsonShape() {
            return Map.of("id", userId);
        }
    }

    public record ByCommentId(String commentId) implements Recipient {
        @Override
        public Map<String, Object> toJsonShape() {
            return Map.of("comment_id", commentId);
        }
    }

    public record AccessTokenContext(
            String instagramBusinessAccountId,
            String pageAccessToken
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String instagramBusinessAccountId;
            private String pageAccessToken;

            public Builder instagramBusinessAccountId(String instagramBusinessAccountId) {
                this.instagramBusinessAccountId = instagramBusinessAccountId;
                return this;
            }

            public Builder pageAccessToken(String pageAccessToken) {
                this.pageAccessToken = pageAccessToken;
                return this;
            }

            public AccessTokenContext build() {
                return new AccessTokenContext(instagramBusinessAccountId, pageAccessToken);
            }
        }
    }

    public record SendResult(boolean success, String messageId, String error, int httpStatus) {
        public static SendResult success(String mid) {
            return new SendResult(true, mid, null, 200);
        }

        public static SendResult failure(String error, int status) {
            return new SendResult(false, null, error, status);
        }
    }
}