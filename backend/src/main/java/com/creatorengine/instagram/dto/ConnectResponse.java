package com.creatorengine.instagram.dto;

/** Returned by {@code GET /api/instagram/connect}. */
public record ConnectResponse(
        /** The Meta authorization URL the frontend should redirect to. */
        String authUrl
) {}
