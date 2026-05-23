package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Shape of {@code GET /v19.0/me/accounts}.
 *
 * <p>Returns the Facebook Pages the user manages. We pick the first
 * one that has a linked {@code instagram_business_account} — that's
 * the account we'll automate against. A future iteration could let
 * the user pick when there's more than one.</p>
 */
public record MetaPagesResponse(
        List<Page> data
) {
    public record Page(
            String id,
            String name,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("instagram_business_account") IgBusinessRef instagramBusinessAccount
    ) {}

    public record IgBusinessRef(String id) {}
}
