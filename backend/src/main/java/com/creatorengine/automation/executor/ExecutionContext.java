package com.creatorengine.automation.executor;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.InstagramAccount;

/**
 * What an action handler needs to do its job. Bundled into one
 * record so the engine can pass it through without juggling four
 * positional arguments.
 *
 * @param uid                CreatorEngine owner of the automation
 * @param automation         the matched automation
 * @param event              the webhook event that triggered the match
 * @param connectedAccount   the user's connected IG account (null = not connected;
 *                           SAVE_CONTACT still works, but no DMs can be sent)
 */
public record ExecutionContext(
        String uid,
        Automation automation,
        WebhookEventDto event,
        InstagramAccount connectedAccount
) {}
