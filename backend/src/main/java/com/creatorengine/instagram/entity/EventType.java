package com.creatorengine.instagram.entity;

/** The webhook event flavours we currently parse. */
public enum EventType {
    COMMENT,
    DM,
    STORY_REPLY,
    CONTENT_SHARED,   // someone shared a post/reel into your DM
    LIVE_COMMENT,     // comment during an Instagram Live broadcast
    STORY_MENTION     // someone mentioned @youraccount in their story
}
