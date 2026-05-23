package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends Instagram direct messages via Meta's Messaging API.
 *
 * <p>Endpoint:
 * {@code POST https://graph.facebook.com/{version}/{ig-business-account-id}/messages}
 * with a JSON body of {@code { "recipient": {...}, "message": {...} }}
 * and the page access token in a query param.</p>
 *
 * <p>The recipient shape depends on what we're replying to:</p>
 * <ul>
 *   <li><b>Comment trigger</b> — {@code {"comment_id": "..."}} (Private
 *       Replies API). This is the ONLY way to DM someone who has only
 *       commented; using their user id directly returns "outside 24-hour
 *       window".</li>
 *   <li><b>DM / story reply</b> — {@code {"id": "<sender_ig_id>"}}.</li>
 * </ul>
 *
 * <p><b>Retry placeholder</b>: this is the right place to wrap with
 * {@code @Retryable} (Spring Retry) using exponential backoff on
 * 5xx + 429. See the TODO comment in {@link #post}.</p>
 *
 * <p><b>Rate-limit placeholder</b>: Meta's per-app and per-user
 * rate limits are well-documented; production should funnel calls
 * through a {@code Semaphore}-backed limiter or a token-bucket
 * keyed on {@code pageAccessToken}. Out of scope for the MVP.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaMessagingService {

    private final AppProperties props;
    private volatile RestClient client;

    @PostConstruct
    void init() {
        if (props.getMeta() == null || props.getMeta().getAppId() == null) {
            log.warn("⚠️  Meta integration not fully configured; DMs will fail at send time.");
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

    // ─── Public API ──────────────────────────────────────────
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
        body.put("message",   Map.of("text", text));

        return post(ctx.instagramBusinessAccountId(), ctx.pageAccessToken(), body);
    }

    // ─── HTTP plumbing ───────────────────────────────────────
    private SendResult post(String igAccountId, String pageAccessToken, Map<String, Object> body) {
        // TODO retry: wrap this call with @Retryable on 429 / 5xx and
        //             exponential backoff. Add a CircuitBreaker once we
        //             have metrics in place. For now, single attempt.
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
            log.info("DM sent ig={} → message_id={}", igAccountId, messageId);
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

    // ─── Value types ─────────────────────────────────────────

    /** Tagged union over the two ways to address a recipient. */
    public sealed interface Recipient permits ByUserId, ByCommentId {
        Map<String, Object> toJsonShape();
    }

    public record ByUserId(String userId) implements Recipient {
        @Override public Map<String, Object> toJsonShape() { return Map.of("id", userId); }
    }

    public record ByCommentId(String commentId) implements Recipient {
        @Override public Map<String, Object> toJsonShape() { return Map.of("comment_id", commentId); }
    }

    @Builder
    public record AccessTokenContext(
            /** The brand's IG business account id (path segment in the URL). */
            String instagramBusinessAccountId,
            /** Page access token (long-lived, stored at connect time). */
            String pageAccessToken
    ) {}

    @Builder
    public record SendResult(boolean success, String messageId, String error, int httpStatus) {
        public static SendResult success(String mid)        { return new SendResult(true,  mid,  null,  200); }
        public static SendResult failure(String e, int s)   { return new SendResult(false, null, e,    s);   }
    }
}
