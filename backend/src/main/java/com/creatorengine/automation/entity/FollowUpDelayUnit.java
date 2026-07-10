package com.creatorengine.automation.entity;

/** Unit for the single no-reply follow-up delay. */
public enum FollowUpDelayUnit {
    MINUTES,
    HOURS,
    DAYS;

    public long toMillis(int amount) {
        return switch (this) {
            case MINUTES -> amount * 60_000L;
            case HOURS -> amount * 60L * 60_000L;
            case DAYS -> amount * 24L * 60L * 60_000L;
        };
    }
}