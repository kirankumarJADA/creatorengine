package com.creatorengine.automation.entity;

/**
 * How an automation chooses which IG post(s) it applies to.
 *
 * <ul>
 *   <li>{@code ALL} — fire for comments on every post.</li>
 *   <li>{@code SPECIFIC} — fire only for one chosen post (see {@code targetPostId}).</li>
 *   <li>{@code NEXT_POST} — wait for the user's next upload after creation, then lock
 *       onto that post forever (becomes equivalent to SPECIFIC once locked).</li>
 * </ul>
 */
public enum PostTargetMode {
    ALL,
    SPECIFIC,
    NEXT_POST
}