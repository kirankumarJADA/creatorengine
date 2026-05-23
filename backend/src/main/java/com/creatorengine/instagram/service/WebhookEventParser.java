package com.creatorengine.instagram.service;

import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Flattens Meta's webhook payload into our canonical
 * {@link WebhookEventDto} shape.
 *
 * <p>Meta groups events by IG business account ("entry") and then by
 * subscription kind ("changes" for field-style events like comments,
 * "messaging" for DM-style events). One incoming POST can carry many
 * events for many accounts. We walk both arrays and emit one DTO per
 * event, regardless of which sub-shape it came from.</p>
 *
 * <p>This is intentionally lenient about missing fields — Meta has a
 * habit of adding optional fields without warning, and we never want
 * the whole batch to fail because one event was shaped oddly. Bad
 * shapes get logged and skipped.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventParser {

    private final ObjectMapper objectMapper;

    public List<WebhookEventDto> parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return List.of();
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
        if (!entry.isArray()) return out;

        for (JsonNode e : entry) {
            String accountId = text(e.path("id"));
            long entryTimeSec = e.path("time").asLong(0);

            // Field-style events (comments, story_insights, etc.)
            JsonNode changes = e.path("changes");
            if (changes.isArray()) {
                for (JsonNode change : changes) {
                    WebhookEventDto dto = parseChange(change, accountId, entryTimeSec);
                    if (dto != null) out.add(dto);
                }
            }

            // Messaging-style events (DMs, story replies)
            JsonNode messaging = e.path("messaging");
            if (messaging.isArray()) {
                for (JsonNode m : messaging) {
                    WebhookEventDto dto = parseMessaging(m, accountId);
                    if (dto != null) out.add(dto);
                }
            }
        }
        return out;
    }

    // ─── change-style: comments ──────────────────────────────
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
                    // The comment's own id — needed for the Private Replies API.
                    .commentId(text(value.path("id")))
                    .eventTime(secsToInstant(entryTimeSec))
                    .receivingAccountId(accountId)
                    .build();
        }
        // Other change fields (mentions, story_insights, etc.) are not
        // wired up for the MVP. Log so we know they're arriving.
        log.debug("Ignoring change field '{}'.", field);
        return null;
    }

    // ─── messaging-style: DMs + story replies ────────────────
    private WebhookEventDto parseMessaging(JsonNode m, String accountId) {
        JsonNode sender   = m.path("sender");
        JsonNode message  = m.path("message");
        long timestampMs  = m.path("timestamp").asLong(0);

        if (message.isMissingNode() || message.isNull()) {
            log.debug("Skipping non-message event in messaging[].");
            return null;
        }

        // Story replies are messages with a `reply_to.story` field.
        boolean isStoryReply = !message.path("reply_to").path("story").isMissingNode();
        EventType type = isStoryReply ? EventType.STORY_REPLY : EventType.DM;

        // Username isn't reliably included — frontend resolves it via a
        // profile lookup later if needed. For now we surface the sender id.
        return WebhookEventDto.builder()
                .type(type)
                .message(text(message.path("text")))
                .username(text(sender.path("username")))   // often null
                .instagramUserId(text(sender.path("id")))
                .postId(isStoryReply
                        ? text(message.path("reply_to").path("story").path("id"))
                        : null)
                .messageId(text(message.path("mid")))        // Meta's mid — dedup key for DM/story
                .eventTime(msToInstant(timestampMs))
                .receivingAccountId(accountId)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────
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
