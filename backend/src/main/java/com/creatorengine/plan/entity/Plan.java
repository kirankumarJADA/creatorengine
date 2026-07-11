package com.creatorengine.plan.entity;

/**
 * Subscription plan tiers. Controls how many Instagram accounts
 * a user can connect. No payment system exists yet — the plan is
 * set manually (or defaults to FREE). Wire Stripe/Paddle later by
 * updating the plan field on the user doc via a webhook handler.
 */
public enum Plan {
    FREE,
    PRO,
    AGENCY;

    public int maxInstagramAccounts() {
        return switch (this) {
            case FREE   -> 2;
            case PRO    -> 10;
            case AGENCY -> Integer.MAX_VALUE;
        };
    }

    public String displayName() {
        return switch (this) {
            case FREE   -> "Free";
            case PRO    -> "Pro";
            case AGENCY -> "Agency";
        };
    }
}