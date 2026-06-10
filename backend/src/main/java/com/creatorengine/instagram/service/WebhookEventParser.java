package com.creatorengine.instagram.service;

import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebhookEventParser {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventParser.class);

    private final ObjectMapper objectMapper;

    public WebhookEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<WebhookEventDto> parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            return parse(root);
        } catch (Exception e) {
            log.warn("Failed to parse webhook payload: {}", e.getMessage());
            return List.of();
        }
    }

    public List<WebhookEventDto> parse(JsonNode root) {
        List<WebhookEventDto> out = new ArrayList<>();
        JsonNode entry = root.path("entry");

        if (!entry.isArray()) {
            return out;
        }

        for (JsonNode e : entry) {
            String accountId = text(e.path("id"));
            long entryTimeSec = e.path("time").asLong(0);

            JsonNode changes = e.path("changes");
            if (changes.isArray()) {
                for (JsonNode change : changes) {
                    WebhookEventDto dto = parseChange(change, accountId, entryTimeSec);
                    if (dto != null) {
                        out.add(dto);
                    }
                }
            }

            JsonNode messaging = e.path("messaging");
            if (messaging.isArray()) {
                for (JsonNode m : messaging) {
                    WebhookEventDto dto = parseMessaging(m, accountId);
                    if (dto != null) {
                        out.add(dto);
                    }
                }
            }
        }

        return out;
    }

    private WebhookEventDto parseChange(JsonNode change, String accountId, long entryTimeSec) {
        String field = text(change.path("field"));
        JsonNode value = change.path("value");

        if ("comments".equals(field)) {
            return WebhookEventDto.builder()
                    .type(EventType.COMMENT)
                    .message(text(value.path("text")))
                    .username(text(value.path("from").path("username")))
                    .instagramUserId(text(value.path("from").path("id")))
                    .postId(text(value.path("media").path("id")))
                    .commentId(text(value.path("id")))
                    .eventTime(secsToInstant(entryTimeSec))
                    .receivingAccountId(accountId)
                    .build();
        }

        log.debug("Ignoring change field '{}'.", field);
        return null;
    }

    private WebhookEventDto parseMessaging(JsonNode m, String accountId) {
        JsonNode sender = m.path("sender");
        JsonNode message = m.path("message");
        long timestampMs = m.path("timestamp").asLong(0);

        if (message.isMissingNode() || message.isNull()) {
            log.debug("Skipping non-message event in messaging[].");
            return null;
        }

        boolean isStoryReply = !message.path("reply_to").path("story").isMissingNode();
        EventType type = isStoryReply ? EventType.STORY_REPLY : EventType.DM;

        // When a user taps a quick-reply button, the message carries the
        // hidden payload we set on the button (e.g. "fgate:<automationId>").
        String quickReplyPayload = text(message.path("quick_reply").path("payload"));

        return WebhookEventDto.builder()
                .type(type)
                .message(text(message.path("text")))
                .username(text(sender.path("username")))
                .instagramUserId(text(sender.path("id")))
                .postId(isStoryReply
                        ? text(message.path("reply_to").path("story").path("id"))
                        : null)
                .messageId(text(message.path("mid")))
                .quickReplyPayload(quickReplyPayload)
                .eventTime(msToInstant(timestampMs))
                .receivingAccountId(accountId)
                .build();
    }

    private static String text(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? null : n.asText();
    }

    private static Instant secsToInstant(long secs) {
        return secs > 0 ? Instant.ofEpochSecond(secs) : null;
    }

    private static Instant msToInstant(long ms) {
        return ms > 0 ? Instant.ofEpochMilli(ms) : null;
    }
}