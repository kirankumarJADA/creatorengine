package com.creatorengine.automation.executor;

import com.creatorengine.automation.email.EmailCollectionService;
import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.followup.FollowUpService;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.instagram.entity.EventType;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.MetaMessagingService;
import com.creatorengine.instagram.service.MetaMessagingService.AccessTokenContext;
import com.creatorengine.instagram.service.MetaMessagingService.ByCommentId;
import com.creatorengine.instagram.service.MetaMessagingService.ByUserId;
import com.creatorengine.instagram.service.MetaMessagingService.QuickReply;
import com.creatorengine.instagram.service.MetaMessagingService.Recipient;
import com.creatorengine.instagram.service.MetaMessagingService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private static final int MAX_REPLIED_KEYS = 5_000;
    private final ConcurrentHashMap<String, Boolean> publicReplyDone = new ConcurrentHashMap<>();

    private final TemplateRenderer templateRenderer;
    private final MetaMessagingService metaMessaging;
    private final ContactService contactService;
    private final FollowUpService followUpService;
    private final EmailCollectionService emailCollectionService;

    public ActionExecutor(
            TemplateRenderer templateRenderer,
            MetaMessagingService metaMessaging,
            ContactService contactService,
            FollowUpService followUpService,
            EmailCollectionService emailCollectionService
    ) {
        this.templateRenderer = templateRenderer;
        this.metaMessaging = metaMessaging;
        this.contactService = contactService;
        this.followUpService = followUpService;
        this.emailCollectionService = emailCollectionService;
    }

    public ExecutionResult execute(ExecutionContext ctx, Automation.Action action) {
        if (action == null || action.getType() == null) {
            return ExecutionResult.failed(null, "Action is missing or has no type.");
        }

        // ---------------------------------------------------------------
        // BOT PROTECTION: per-action random jitter delay
        // Applied before every send action when bot protection is enabled.
        // Randomizing timing between sends avoids predictable patterns
        // that Instagram may flag as automated/bot-like behavior.
        // ---------------------------------------------------------------
        applyBotProtectionJitter(ctx.automation());

        String messageTemplate = pickMessageTemplate(action);
        String rendered = templateRenderer.renderWithUsername(
                messageTemplate,
                ctx.event() != null ? ctx.event().username() : null
        );

        boolean hasImage = action.getImageUrl() != null && !action.getImageUrl().isBlank();

        if ((action.getType() == ActionType.SEND_DM
                || action.getType() == ActionType.SEND_MESSAGE
                || action.getType() == ActionType.SEND_LINK)
                && (rendered == null || rendered.isBlank())
                && !hasImage) {
            log.warn("Automation action {} has an empty message template and no image - failing early.",
                    action.getType());
            return ExecutionResult.failed(null,
                    "Empty recipient or message. Edit your automation and add a message or image.");
        }

        return switch (action.getType()) {
            case SEND_DM -> sendDirect(ctx, rendered, action.getImageUrl());
            case SEND_MESSAGE -> sendDirect(ctx, rendered, action.getImageUrl());
            case SEND_LINK -> sendDirect(ctx, appendLink(rendered, action.getLink()), action.getImageUrl());
            case SAVE_CONTACT -> saveContactOnly(ctx, rendered);
            case DELAY -> ExecutionResult.failed(null,
                    "DELAY must be handled by the engine, not executed inline.");
        };
    }

    public ExecutionResult execute(ExecutionContext ctx) {
        Automation automation = ctx.automation();
        if (automation == null) {
            return ExecutionResult.failed(null, "No automation in context.");
        }

        var actions = automation.getEffectiveActions();
        if (actions.isEmpty()) {
            return ExecutionResult.failed(null, "Automation has no actions configured.");
        }

        return execute(ctx, actions.get(0));
    }

    public ExecutionResult executeFollowGateAsk(ExecutionContext ctx) {
        InstagramAccount acct = ctx.connectedAccount();
        if (acct == null) {
            return ExecutionResult.failed(null, "Instagram account not connected.");
        }

        var event = ctx.event();
        if (event == null) {
            return ExecutionResult.failed(null, "Follow gate requires an event.");
        }

        Automation automation = ctx.automation();

        // BOT PROTECTION jitter before follow-gate ask
        applyBotProtectionJitter(automation);

        String askText = templateRenderer.renderWithUsername(
                automation.getFollowGateMessage(),
                event.username()
        );
        if (askText == null || askText.isBlank()) {
            return ExecutionResult.failed(null, "Follow-gate message is empty.");
        }

        String buttonLabel = automation.getFollowGateButtonLabel();
        if (buttonLabel == null || buttonLabel.isBlank()) {
            buttonLabel = "I Followed \u2705";
        }

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        QuickReply button = new QuickReply(buttonLabel, "fgate:" + automation.getId());

        // For COMMENT events: send as a private reply to the comment (inline notification).
        // For DM-based events (DM, STORY_REPLY, CONTENT_SHARED): send a plain DM by user id.
        Recipient recipient;
        if (event.type() == EventType.COMMENT
                && event.commentId() != null && !event.commentId().isBlank()) {
            recipient = new ByCommentId(event.commentId());
        } else {
            String senderId = event.instagramUserId();
            if (senderId == null || senderId.isBlank()) {
                return ExecutionResult.failed(null,
                        "Follow gate: no sender id available for this event type.");
            }
            recipient = new ByUserId(senderId);
        }

        SendResult result = metaMessaging.sendTextWithQuickReplies(
                recipient,
                askText,
                List.of(button),
                tokenCtx
        );

        if (result.success()) {
            contactService.recordFromEvent(ctx.uid(), event, askText);
            maybePostPublicReply(ctx, tokenCtx);
            return ExecutionResult.sent(askText, result.messageId());
        }

        return ExecutionResult.failed(askText, result.error(), result.httpStatus());
    }

    public ExecutionResult executePublicReplyOnly(ExecutionContext ctx) {
        InstagramAccount acct = ctx.connectedAccount();
        if (acct == null) {
            return ExecutionResult.failed(null, "Instagram account not connected.");
        }

        var event = ctx.event();
        if (event == null || event.type() != EventType.COMMENT) {
            return ExecutionResult.failed(null, "Public reply requires a COMMENT event.");
        }

        String commentId = event.commentId();
        if (commentId == null || commentId.isBlank()) {
            return ExecutionResult.failed(null, "Comment id missing.");
        }

        Automation automation = ctx.automation();
        if (automation == null || !automation.getPublicReplyEnabled()) {
            return ExecutionResult.failed(null, "Public reply not enabled.");
        }

        List<String> active = activeReplyTexts(automation);
        if (active.isEmpty()) {
            return ExecutionResult.failed(null, "No active public reply templates.");
        }

        String dedupKey = automation.getId() + ":" + commentId;
        if (publicReplyDone.putIfAbsent(dedupKey, Boolean.TRUE) != null) {
            log.debug("Public reply already posted for {}, skipping.", dedupKey);
            return ExecutionResult.failed(null, "Public reply already posted.");
        }
        evictIfFull();

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        String template = active.get(ThreadLocalRandom.current().nextInt(active.size()));
        String reply = templateRenderer.renderWithUsername(template, event.username());

        try {
            SendResult result = metaMessaging.replyToComment(commentId, reply, tokenCtx);
            if (result.success()) {
                log.info("Public-reply-only posted on comment_id={} (automation={})",
                        commentId, automation.getId());
                contactService.recordFromEvent(ctx.uid(), event, reply);
                return ExecutionResult.sent(reply, result.messageId());
            }
            publicReplyDone.remove(dedupKey);
            return ExecutionResult.failed(reply, result.error(), result.httpStatus());
        } catch (Exception ex) {
            publicReplyDone.remove(dedupKey);
            log.warn("Public-reply-only threw for comment_id={}: {}", commentId, ex.getMessage());
            return ExecutionResult.failed(reply, "Public reply exception: " + ex.getMessage(), null);
        }
    }

    // ---------------------------------------------------------------
    // BOT PROTECTION: random jitter sleep
    // Sleeps for a random duration between min and max delay seconds
    // configured on the automation. Only applied when bot protection
    // is enabled. Safe to call from any thread - uses Thread.sleep
    // which is fine inside the QueueWorker thread pool.
    // ---------------------------------------------------------------
    private static void applyBotProtectionJitter(Automation automation) {
        if (automation == null || !automation.getBotProtectionEnabled()) {
            return;
        }

        int minMs = Math.max(0, automation.getBotProtectionMinDelaySeconds()) * 1000;
        int maxMs = Math.max(minMs, automation.getBotProtectionMaxDelaySeconds() * 1000);

        if (minMs == maxMs) {
            return; // No range, no jitter
        }

        int jitterMs = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);

        try {
            log.debug("Bot protection jitter: sleeping {}ms (range {}-{}ms)",
                    jitterMs, minMs, maxMs);
            Thread.sleep(jitterMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.debug("Bot protection jitter interrupted.");
        }
    }

    private ExecutionResult sendDirect(ExecutionContext ctx, String message) {
        return sendDirect(ctx, message, null);
    }

    private ExecutionResult sendDirect(ExecutionContext ctx, String message, String imageUrl) {
        InstagramAccount acct = ctx.connectedAccount();
        if (acct == null) {
            return ExecutionResult.failed(message, "Instagram account not connected.");
        }

        Recipient recipient = recipientFor(ctx);
        if (recipient == null) {
            return ExecutionResult.failed(message,
                    "Cannot derive recipient - missing comment id or sender id.");
        }

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        boolean hasText = message != null && !message.isBlank();

        SendResult imageResult = null;
        if (hasImage) {
            imageResult = metaMessaging.sendImage(recipient, imageUrl, tokenCtx);
            if (!imageResult.success()) {
                log.warn("DM image send failed: {}", imageResult.error());
            }
        }

        if (!hasText) {
            if (imageResult != null && imageResult.success()) {
                contactService.recordFromEvent(ctx.uid(), ctx.event(), "[image]");
                maybePostPublicReply(ctx, tokenCtx);
                maybeScheduleFollowUp(ctx);
                maybeScheduleEmailCapture(ctx, recipient, tokenCtx);
                return ExecutionResult.sent("[image]", imageResult.messageId());
            }
            return ExecutionResult.failed(null,
                    imageResult != null ? imageResult.error() : "Nothing to send.",
                    imageResult != null ? imageResult.httpStatus() : 0);
        }

        SendResult result = metaMessaging.sendText(recipient, message, tokenCtx);

        if (result.success()) {
            contactService.recordFromEvent(ctx.uid(), ctx.event(), message);
            maybePostPublicReply(ctx, tokenCtx);
            maybeScheduleFollowUp(ctx);
            maybeScheduleEmailCapture(ctx, recipient, tokenCtx);
            return ExecutionResult.sent(message, result.messageId());
        }

        return ExecutionResult.failed(message, result.error(), result.httpStatus());
    }

    /**
     * FOLLOW-UP MESSAGE (single no-reply follow-up)
     * Called after every successful DM send. If the automation has
     * followUpEnabled, (re)schedules the single follow-up timer keyed to
     * this contact - each successful send in a multi-step chain simply
     * resets the timer to start from the most recent message, per spec.
     */
    private void maybeScheduleFollowUp(ExecutionContext ctx) {
        Automation automation = ctx.automation();
        var event = ctx.event();
        if (automation == null || event == null) return;

        String instagramUserId = event.instagramUserId();
        if (instagramUserId == null || instagramUserId.isBlank()) return;

        // Pass full event so FollowUpService can store username + igAccountId
        followUpService.scheduleOrReset(ctx.uid(), automation, event);
    }

    /**
     * EMAIL COLLECTION
     * Called after every successful DM send. If the automation has
     * emailCollectEnabled, registers an expectation so the engine can
     * capture the contact's email when they reply. If the creator also
     * set an emailCollectMessage, that text is sent as an extra DM
     * prompting the user to share their email.
     */
    private void maybeScheduleEmailCapture(
            ExecutionContext ctx,
            Recipient recipient,
            AccessTokenContext tokenCtx
    ) {
        Automation automation = ctx.automation();
        var event = ctx.event();
        if (automation == null || !automation.getEmailCollectEnabled() || event == null) return;

        // Send the "ask for email" message if configured
        String askMsg = automation.getEmailCollectMessage();
        if (askMsg != null && !askMsg.isBlank()) {
            String rendered = templateRenderer.renderWithUsername(askMsg, event.username());
            SendResult askResult = metaMessaging.sendText(recipient, rendered, tokenCtx);
            if (!askResult.success()) {
                log.warn("Email-ask DM failed for automation {}: {}", automation.getId(), askResult.error());
            }
        }

        // Register expectation so the next DM from this contact is checked for an email
        emailCollectionService.scheduleExpectation(ctx.uid(), automation, event);
    }

    private void maybePostPublicReply(ExecutionContext ctx, AccessTokenContext tokenCtx) {
        var event = ctx.event();
        if (event == null || event.type() != EventType.COMMENT) return;

        Automation automation = ctx.automation();
        if (automation == null || !automation.getPublicReplyEnabled()) return;

        String commentId = event.commentId();
        if (commentId == null || commentId.isBlank()) return;

        String dedupKey = automation.getId() + ":" + commentId;
        if (publicReplyDone.putIfAbsent(dedupKey, Boolean.TRUE) != null) {
            log.debug("Public reply already posted for {}, skipping.", dedupKey);
            return;
        }
        evictIfFull();

        List<String> active = activeReplyTexts(automation);
        if (active.isEmpty()) return;

        String template = active.get(ThreadLocalRandom.current().nextInt(active.size()));
        String reply = templateRenderer.renderWithUsername(template, event.username());

        try {
            SendResult result = metaMessaging.replyToComment(commentId, reply, tokenCtx);
            if (result.success()) {
                log.info("Public reply posted on comment_id={} (automation={})",
                        commentId, automation.getId());
            } else {
                log.warn("Public reply failed for comment_id={}: {}", commentId, result.error());
                publicReplyDone.remove(dedupKey);
            }
        } catch (Exception ex) {
            log.warn("Public reply threw for comment_id={}: {}", commentId, ex.getMessage());
            publicReplyDone.remove(dedupKey);
        }
    }

    private void evictIfFull() {
        if (publicReplyDone.size() > MAX_REPLIED_KEYS) {
            int target = MAX_REPLIED_KEYS / 2;
            for (String k : publicReplyDone.keySet()) {
                if (publicReplyDone.size() <= target) break;
                publicReplyDone.remove(k);
            }
        }
    }

    private static List<String> activeReplyTexts(Automation automation) {
        var replies = automation.getPublicReplies();
        if (replies == null || replies.isEmpty()) return List.of();
        return replies.stream()
                .filter(r -> r != null && r.getEnabled())
                .map(Automation.PublicReply::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    private ExecutionResult saveContactOnly(ExecutionContext ctx, String renderedMessage) {
        String lastMessage = renderedMessage != null && !renderedMessage.isBlank()
                ? renderedMessage
                : (ctx.event() != null ? ctx.event().message() : null);
        contactService.recordFromEvent(ctx.uid(), ctx.event(), lastMessage);
        return ExecutionResult.savedOnly(lastMessage);
    }

    private Recipient recipientFor(ExecutionContext ctx) {
        var event = ctx.event();
        if (event == null) return null;

        if (event.type() == EventType.COMMENT) {
            String commentId = event.commentId();
            if (commentId == null || commentId.isBlank()) {
                log.warn("COMMENT event has no commentId - cannot send Private Reply.");
                return null;
            }
            return new ByCommentId(commentId);
        }

        String senderId = event.instagramUserId();
        if (senderId == null || senderId.isBlank()) return null;
        return new ByUserId(senderId);
    }

    private static String pickMessageTemplate(Automation.Action action) {
        List<String> pool = new java.util.ArrayList<>();

        if (action.getMessage() != null && !action.getMessage().isBlank()) {
            pool.add(action.getMessage());
        }

        if (action.getVariations() != null) {
            action.getVariations().stream()
                    .filter(v -> v != null && !v.isBlank())
                    .forEach(pool::add);
        }

        if (pool.isEmpty()) return action.getMessage();
        if (pool.size() == 1) return pool.get(0);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static String appendLink(String message, String link) {
        if (link == null || link.isBlank()) return message;
        if (message == null || message.isBlank()) return link;
        return message.contains(link) ? message : message + "\n" + link;
    }
}