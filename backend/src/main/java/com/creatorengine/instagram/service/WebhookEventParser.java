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

        if ("comments".equals(field) || "live_comments".equals(field)) {
            boolean isLive = "live_comments".equals(field);
            String fromId = text(value.path("from").path("id"));

            // ---------------------------------------------------------------
            // FIX: Reject comment events where Instagram did not include the
            // commenter's user id. This happens for private/restricted accounts
            // and certain ad comments. Without a fromId we cannot send a
            // Private Reply, so there is no point dispatching the event — it
            // would only produce "Empty recipient" failures downstream.
            // ---------------------------------------------------------------
            if (fromId == null || fromId.isBlank()) {
                log.warn("Comment webhook missing from.id - skipping (private/restricted account or ad comment).");
                return null;
            }

            // ---------------------------------------------------------------
            // CRITICAL SAFETY FILTER (mirror of the DM "is_echo" guard below)
            // Instagram also notifies us about comments the OWNER's own
            // account posted (including this app's public replies to other
            // comments). If we treated those as incoming events, our public
            // replies would trigger the same automation again, and we'd reply
            // to our own reply forever — an infinite spam loop.
            // NEVER react to comments authored by the owning IG account.
            // ---------------------------------------------------------------
            if (accountId != null && fromId.equals(accountId)) {
                log.debug("Skipping comment authored by the owning account (self-comment / our own reply).");
                return null;
            }

            return WebhookEventDto.builder()
                    .type(isLive ? EventType.LIVE_COMMENT : EventType.COMMENT)
                    .message(text(value.path("text")))
                    .username(text(value.path("from").path("username")))
                    .instagramUserId(fromId)
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

        // ---------------------------------------------------------------
        // CRITICAL SAFETY FILTER
        // Instagram also notifies us about messages the OWNER's own account
        // SENT (not just received). Those carry "is_echo": true. If we treated
        // them as incoming events, the owner's normal outgoing messages would
        // trigger DM automations and create loops / unwanted auto-DMs.
        // NEVER react to our own sent messages.
        // ---------------------------------------------------------------
        if (message.path("is_echo").asBoolean(false)) {
            log.debug("Skipping message echo (owner-sent message).");
            return null;
        }

        String senderId = text(sender.path("id"));

        // Extra guard: if the sender IS the receiving (owner) account, skip it.
        // We only ever react to messages FROM other users.
        if (senderId != null && senderId.equals(accountId)) {
            log.debug("Skipping message where sender == owning account.");
            return null;
        }

        boolean isStoryReply = !message.path("reply_to").path("story").isMissingNode();

        // Detect content-shared events.
        // Instagram sends different attachment types depending on what was shared:
        //   "share"    — a post/reel shared from someone's profile
        //   "reel"     — a reel shared directly
        //   "ig_reel"  — same, alternate type name seen in production
        //   "ig_post"  — static post, payload URL is a lookaside CDN URL (not instagram.com)
        //   "video"    — video attachment (can also be a shared reel)
        // We detect a "share" if:
        //   (a) the attachment type is one of the above, OR
        //   (b) the payload URL contains an instagram.com/p/ or /reel/ path.
        boolean isContentShared = false;
        String sharedPostId = null;
        if (!isStoryReply) {
            JsonNode attachments = message.path("attachments");
            if (attachments.isArray() && !attachments.isEmpty()) {
                // Log all attachment types to aid debugging
                for (JsonNode att : attachments) {
                    log.info("DM attachment type='{}' payloadUrl='{}' payloadId='{}'",
                            text(att.path("type")),
                            text(att.path("payload").path("url")),
                            text(att.path("payload").path("id")));
                }
                // Now check each attachment
                for (JsonNode att : attachments) {
                    String attType = text(att.path("type"));
                    String payloadUrl = text(att.path("payload").path("url"));

                    boolean typeIsShare = "share".equals(attType)
                            || "reel".equals(attType)
                            || "ig_reel".equals(attType)
                            || "ig_post".equals(attType);

                    // Also detect via payload URL even if type is missing/unknown
                    boolean urlIsPost = payloadUrl != null
                            && (payloadUrl.contains("instagram.com/p/")
                                || payloadUrl.contains("instagram.com/reel/")
                                || payloadUrl.contains("instagram.com/tv/"));

                    if (typeIsShare || urlIsPost) {
                        isContentShared = true;
                        String payloadId = text(att.path("payload").path("id"));
                        if (payloadId != null && !payloadId.isBlank()) {
                            sharedPostId = payloadId;
                        }
                        log.info("CONTENT_SHARED detected via type='{}' url='{}' postId='{}'",
                                attType, payloadUrl, sharedPostId);
                        break;
                    }
                }
            }
        }

        EventType type = isStoryReply
                ? EventType.STORY_REPLY
                : isContentShared ? EventType.CONTENT_SHARED : EventType.DM;

        // When a user taps a quick-reply button, the message carries the
        // hidden payload we set on the button (e.g. "fgate:<automationId>").
        String quickReplyPayload = text(message.path("quick_reply").path("payload"));

        return WebhookEventDto.builder()
                .type(type)
                .message(text(message.path("text")))
                .username(text(sender.path("username")))
                .instagramUserId(senderId)
                .postId(isStoryReply
                        ? text(message.path("reply_to").path("story").path("id"))
                        : sharedPostId)
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