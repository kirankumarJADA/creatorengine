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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dispatches an automation's action against a webhook event.
 *
 * <p>One method per action type, kept inline in a switch because we
 * only have four — splitting them into per-handler classes would
 * add four files without buying anything until we have a fifth.
 * If a fifth action ever appears, extract a Strategy map.</p>
 *
 * <p>Concrete responsibilities:</p>
 * <ul>
 *   <li>Render the message template (delegated to {@link TemplateRenderer}).</li>
 *   <li>Pick the right recipient shape based on event type
 *       (comment id for COMMENT, sender user id otherwise).</li>
 *   <li>Append the action's link to the message for {@link ActionType#SEND_LINK}.</li>
 *   <li>Always upsert the contact via {@link ContactService} on success —
 *       this gives the Contacts page real data, and means SAVE_CONTACT is
 *       just "do step 5 without sending anything".</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionExecutor {

    private final TemplateRenderer templateRenderer;
    private final MetaMessagingService metaMessaging;
    private final ContactService contactService;

    /**
     * Execute a single action against the event in the context.
     *
     * <p>The {@code action} parameter carries its own message/link/delay —
     * NOT the automation-level legacy fields. This makes the executor
     * blind to whether the automation is single-step (legacy) or
     * multi-step (new): the engine iterates the effective chain and
     * passes each action here independently.</p>
     *
     * <p>DELAY is NOT a valid input to this method — the engine
     * intercepts it before reaching here, because the delay
     * mechanism (re-enqueueing a continuation) needs the queue, which
     * lives outside the executor's reach. Defending against it anyway
     * makes the failure mode loud rather than silent.</p>
     */
    public ExecutionResult execute(ExecutionContext ctx, Automation.Action action) {
        if (action == null || action.getType() == null) {
            return ExecutionResult.failed(null, "Action is missing or has no type.");
        }

        String rendered = templateRenderer.renderWithUsername(
                action.getMessage(),
                ctx.event() != null ? ctx.event().username() : null);

        return switch (action.getType()) {
            case SEND_DM      -> sendDirect(ctx, rendered);
            case SEND_MESSAGE -> sendDirect(ctx, rendered);  // see comment in sendDirect
            case SEND_LINK    -> sendDirect(ctx, appendLink(rendered, action.getLink()));
            case SAVE_CONTACT -> saveContactOnly(ctx, rendered);
            case DELAY        -> ExecutionResult.failed(null,
                    "DELAY must be handled by the engine, not executed inline.");
        };
    }

    /**
     * Back-compat shim — legacy callers (tests, hypothetical future
     * code) that don't know about per-action execution see the
     * automation's first effective action.
     */
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

    // ─── DM / Message / Link share the same wire ────────────
    /*
     * SEND_DM and SEND_MESSAGE currently use the same outbound path —
     * Instagram's Messaging API treats both as DMs. The semantic split
     * ("send a message" = reply on the comment thread, vs. "send a DM"
     * = private message) lives in Meta's separate `/{comment-id}/replies`
     * endpoint, which is a future enhancement. Documenting the
     * limitation here keeps the schema honest.
     */
    private ExecutionResult sendDirect(ExecutionContext ctx, String message) {
        InstagramAccount acct = ctx.connectedAccount();
        if (acct == null) {
            return ExecutionResult.failed(message, "Instagram account not connected.");
        }

        Recipient recipient = recipientFor(ctx);
        if (recipient == null) {
            return ExecutionResult.failed(message,
                    "Cannot derive recipient — missing comment id or sender id.");
        }

        AccessTokenContext tokenCtx = AccessTokenContext.builder()
                .instagramBusinessAccountId(acct.getInstagramUserId())
                .pageAccessToken(acct.getAccessToken())
                .build();

        SendResult result = metaMessaging.sendText(recipient, message, tokenCtx);

        if (result.success()) {
            // On success: record/refresh the contact with what we just sent.
            contactService.recordFromEvent(ctx.uid(), ctx.event(), message);
            return ExecutionResult.sent(message, result.messageId());
        }
        return ExecutionResult.failed(message, result.error(), result.httpStatus());
    }

    private ExecutionResult saveContactOnly(ExecutionContext ctx, String renderedMessage) {
        // For SAVE_CONTACT we don't send anything — but we DO want a
        // record. Use either the rendered "note" (if the user wrote
        // one) or the trigger text as `lastMessage`.
        String lastMessage = renderedMessage != null && !renderedMessage.isBlank()
                ? renderedMessage
                : (ctx.event() != null ? ctx.event().message() : null);
        contactService.recordFromEvent(ctx.uid(), ctx.event(), lastMessage);
        return ExecutionResult.savedOnly(lastMessage);
    }

    // ─── Helpers ─────────────────────────────────────────────
    private Recipient recipientFor(ExecutionContext ctx) {
        var event = ctx.event();
        if (event == null) return null;

        if (event.type() == EventType.COMMENT) {
            // Private Replies: we must address by comment_id, not user id.
            String commentId = event.commentId();
            if (commentId == null || commentId.isBlank()) {
                log.warn("COMMENT event has no commentId — cannot send Private Reply.");
                return null;
            }
            return new ByCommentId(commentId);
        }
        // DM / story reply: standard recipient.id
        String senderId = event.instagramUserId();
        if (senderId == null || senderId.isBlank()) return null;
        return new ByUserId(senderId);
    }

    private static String appendLink(String message, String link) {
        if (link == null || link.isBlank()) return message;
        if (message == null || message.isBlank()) return link;
        return message.contains(link) ? message : message + "\n" + link;
    }
}
