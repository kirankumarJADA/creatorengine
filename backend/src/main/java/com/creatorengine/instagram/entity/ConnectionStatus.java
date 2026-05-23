package com.creatorengine.instagram.entity;

/**
 * Lifecycle state for a stored Instagram account.
 *
 * <p>{@code EXPIRED} is computed at read-time by comparing the
 * stored token's expiry against {@code Instant.now()} — we don't
 * try to maintain it as a stored field, since that would require
 * a background job to flip it.</p>
 */
public enum ConnectionStatus {
    NOT_CONNECTED,
    CONNECTED,
    EXPIRED
}
