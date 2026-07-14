package com.creatorengine.automation.entity;

/**
 * What Instagram event kicks off an automation.
 *
 * <ul>
 *   <li>{@code COMMENT} — comment on any post/reel (or a specific one via targetPostId).</li>
 *   <li>{@code DM} — direct message.</li>
 *   <li>{@code STORY_REPLY} — reply to a story.</li>
 *   <li>{@code NEXT_POST} — special: comment on the creator's NEXT uploaded post only.
 *       Internally fires on COMMENT events, but only for the auto-locked targetPostId.</li>
 * </ul>
 */
public enum TriggerType {
    COMMENT,
    DM,
    STORY_REPLY,
    NEXT_POST,
    CONTENT_SHARED   // someone shares a post/reel to your DM inbox
}