package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                            .baseUrl("https://graph.instagram.com/" + props.getMeta().getGraphApiVersion())
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

    /**
     * Send an image attachment as a DM. Instagram fetches the image from
     * `imageUrl` server-side, so it must be a publicly-readable URL (see
     * MediaUploadController, which uploads to Firebase Storage and grants
     * public read access).
     */
    public SendResult sendImage(Recipient recipient, String imageUrl, AccessTokenContext ctx) {
        if (recipient == null || imageUrl == null || imageUrl.isBlank()) {
            return SendResult.failure("Empty recipient or image URL.", 0);
        }

        if (ctx == null
                || ctx.instagramBusinessAccountId() == null
                || ctx.pageAccessToken() == null) {
            return SendResult.failure("Instagram account not connected.", 0);
        }

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("type", "image");
        attachment.put("payload", Map.of("url", imageUrl));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipient", recipient.toJsonShape());
        body.put("message", Map.of("attachment", attachment));

        return post(ctx.instagramBusinessAccountId(), ctx.pageAccessToken(), body);
    }

    /**
     * Send a text DM with quick-reply buttons (e.g. "I Followed ✅"). Each button
     * carries a hidden payload that comes back to our webhook when tapped.
     */
    public SendResult sendTextWithQuickReplies(
            Recipient recipient,
            String text,
            List<QuickReply> quickReplies,
            AccessTokenContext ctx
    ) {
        if (recipient == null || text == null || text.isBlank()) {
            return SendResult.failure("Empty recipient or message.", 0);
        }

        if (ctx == null
                || ctx.instagramBusinessAccountId() == null
                || ctx.pageAccessToken() == null) {
            return SendResult.failure("Instagram account not connected.", 0);
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("text", text);
        if (quickReplies != null && !quickReplies.isEmpty()) {
            message.put("quick_replies", quickReplies.stream()
                    .map(QuickReply::toJsonShape)
                    .toList());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipient", recipient.toJsonShape());
        body.put("message", message);

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

    /**
     * Post a PUBLIC reply on a comment (visible to everyone on the post).
     * Endpoint: POST /{comment-id}/replies?message=...  This is separate from
     * the private DM sent via /{ig-id}/messages. Best-effort: callers should
     * not fail the whole automation if this fails.
     */
    public SendResult replyToComment(String commentId, String text, AccessTokenContext ctx) {
        if (commentId == null || commentId.isBlank() || text == null || text.isBlank()) {
            return SendResult.failure("Empty comment id or reply text.", 0);
        }

        if (ctx == null || ctx.pageAccessToken() == null) {
            return SendResult.failure("Instagram account not connected.", 0);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri(uri -> uri
                            .path("/" + commentId + "/replies")
                            .queryParam("message", text)
                            .queryParam("access_token", ctx.pageAccessToken())
                            .build())
                    .retrieve()
                    .body(Map.class);

            String replyId = response != null
                    ? String.valueOf(response.getOrDefault("id", ""))
                    : null;

            log.info("Public reply posted comment_id={} reply_id={}", commentId, replyId);
            return SendResult.success(replyId);
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("Public reply failed status={} body={}", ex.getStatusCode(), responseBody);
            return SendResult.failure(
                    "Meta returned " + ex.getStatusCode() + ": " + responseBody,
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            log.warn("Public reply failed unexpectedly: {}", ex.getMessage());
            return SendResult.failure(ex.getMessage(), 0);
        }
    }

    /**
     * Resolve the owner of an Instagram media object (e.g. a story that mentioned us).
     * Calls GET /{mediaId}?fields=owner{id,username}&access_token={token}.
     * Returns empty if the call fails or the owner block is absent.
     */
    public Optional<MediaOwner> resolveMediaOwner(String mediaId, String accessToken) {
        if (mediaId == null || mediaId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().get()
                    .uri(uri -> uri
                            .path("/" + mediaId)
                            .queryParam("fields", "owner")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null) return Optional.empty();

            @SuppressWarnings("unchecked")
            Map<String, Object> owner = (Map<String, Object>) response.get("owner");
            if (owner == null) return Optional.empty();

            String id = String.valueOf(owner.getOrDefault("id", ""));
            String username = owner.containsKey("username") ? String.valueOf(owner.get("username")) : null;
            if (id.isBlank()) return Optional.empty();

            log.info("Resolved media owner mediaId={} ownerId={} username={}", mediaId, id, username);
            return Optional.of(new MediaOwner(id, username));
        } catch (Exception ex) {
            log.warn("Failed to resolve media owner mediaId={}: {}", mediaId, ex.getMessage());
            return Optional.empty();
        }
    }

    public record MediaOwner(String id, String username) {}

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

    /** One quick-reply button. Instagram caps the title at 20 chars. */
    public record QuickReply(String title, String payload) {
        public Map<String, Object> toJsonShape() {
            String safeTitle = title == null ? "" : (title.length() > 20 ? title.substring(0, 20) : title);
            return Map.of(
                    "content_type", "text",
                    "title", safeTitle,
                    "payload", payload == null ? "" : payload
            );
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