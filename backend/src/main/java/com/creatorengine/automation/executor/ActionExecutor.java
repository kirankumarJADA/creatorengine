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
import com.creatorengine.instagram.service.MetaMessagingService.Recipient;
import com.creatorengine.instagram.service.MetaMessagingService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            return ExecutionResult.sent(message, result.messageId());
        }

        return ExecutionResult.failed(message, result.error(), result.httpStatus());
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