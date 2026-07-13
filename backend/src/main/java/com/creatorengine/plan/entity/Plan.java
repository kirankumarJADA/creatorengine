package com.creatorengine.plan.entity;

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