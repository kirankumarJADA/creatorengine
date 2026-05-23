package com.creatorengine.auth.entity;

/**
 * Role taxonomy. Add more values here as new tiers/permissions land.
 *
 * <p>Stored as a plain string in Firestore so the doc remains
 * human-readable in the Firebase console.</p>
 */
public enum Role {
    USER,
    ADMIN
}
