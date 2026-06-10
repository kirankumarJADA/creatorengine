package com.creatorengine.automation.executor;

import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
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
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final TemplateRenderer templateRenderer;
    private final MetaMessagingService metaMessaging;
    private final ContactService contactService;

    public ActionExecutor(
            TemplateRenderer templateRenderer,
            MetaMessagingService metaMessaging,
            ContactService contactService
    ) {
        this.templateRenderer = templateRenderer;
        this.metaMessaging = metaMessaging;
        this.contactService = contactService;
    }

    public ExecutionResult execute(ExecutionContext ctx, Automation.Action action) {
        if (action == null || action.getType() == null) {
            return ExecutionResult.failed(null, "Action is missing or has no type.");
        }

        String rendered = templateRenderer.renderWithUsername(
                action.getMessage(),
                ctx.event() != null ? ctx.event().username() : null
        );

        return switch (action.getType()) {
            case SEND_DM -> sendDirect(ctx, rendered);
            case SEND_MESSAGE -> sendDirect(ctx, rendered);
            case SEND_LINK -> sendDirect(ctx, appendLink(rendered, action.getLink()));
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

    /**
     * Follow-gate ASK: post the public reply (if enabled), then send the
     * "please follow" DM with an "I Followed ✅" button as a private reply to
     * the comment. The button's payload carries the automation id so the tap
     * later delivers this automation's content.
     */
    public ExecutionResult executeFollowGateAsk(ExecutionContext ctx) {
        InstagramAccount acct = ctx.connectedAccount();
        if (acct == null) {
            return ExecutionResult.failed(null, "Instagram account not connected.");
        }

        var event = ctx.event();
        if (event == null || event.commentId() == null || event.commentId().isBlank()) {
            return ExecutionResult.failed(null, "Follow gate requires a comment to reply to.");
        }

        Automation automation = ctx.automation();

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

        SendResult result = metaMessaging.sendTextWithQuickReplies(
                new ByCommentId(event.commentId()),
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

    private ExecutionResult sendDirect(ExecutionContext ctx, String message) {
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

        SendResult result = metaMessaging.sendText(recipient, message, tokenCtx);

        if (result.success()) {
            contactService.recordFromEvent(ctx.uid(), ctx.event(), message);
            maybePostPublicReply(ctx, tokenCtx);
            return ExecutionResult.sent(message, result.messageId());
        }

        return ExecutionResult.failed(message, result.error(), result.httpStatus());
    }

    /**
     * For COMMENT-triggered automations with public replies turned on, post one
     * of the automation's *active* reply templates on the comment, in addition
     * to the private DM. Best-effort — a failure here never affects the DM.
     */
    private void maybePostPublicReply(ExecutionContext ctx, AccessTokenContext tokenCtx) {
        var event = ctx.event();
        if (event == null || event.type() != EventType.COMMENT) {
            return;
        }

        Automation automation = ctx.automation();
        if (automation == null || !automation.getPublicReplyEnabled()) {
            return;
        }

        String commentId = event.commentId();
        if (commentId == null || commentId.isBlank()) {
            return;
        }

        List<String> active = activeReplyTexts(automation);
        if (active.isEmpty()) {
            return;
        }

        String template = active.get(ThreadLocalRandom.current().nextInt(active.size()));
        String reply = templateRenderer.renderWithUsername(template, event.username());

        try {
            SendResult result = metaMessaging.replyToComment(commentId, reply, tokenCtx);
            if (result.success()) {
                log.info("Public reply posted on comment_id={}", commentId);
            } else {
                log.warn("Public reply failed for comment_id={}: {}", commentId, result.error());
            }
        } catch (Exception ex) {
            log.warn("Public reply threw for comment_id={}: {}", commentId, ex.getMessage());
        }
    }

    private static List<String> activeReplyTexts(Automation automation) {
        var replies = automation.getPublicReplies();
        if (replies == null || replies.isEmpty()) {
            return List.of();
        }

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
        if (event == null) {
            return null;
        }

        if (event.type() == EventType.COMMENT) {
            String commentId = event.commentId();
            if (commentId == null || commentId.isBlank()) {
                log.warn("COMMENT event has no commentId - cannot send Private Reply.");
                return null;
            }
            return new ByCommentId(commentId);
        }

        String senderId = event.instagramUserId();
        if (senderId == null || senderId.isBlank()) {
            return null;
        }

        return new ByUserId(senderId);
    }

    private static String appendLink(String message, String link) {
        if (link == null || link.isBlank()) {
            return message;
        }

        if (message == null || message.isBlank()) {
            return link;
        }

        return message.contains(link) ? message : message + "\n" + link;
    }
}