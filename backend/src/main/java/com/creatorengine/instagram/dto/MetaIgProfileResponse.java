package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Shape of {@code GET /v19.0/{ig-id}?fields=id,username,name,profile_picture_url}. */
public record MetaIgProfileResponse(
        String id,
        String username,
        String name,
        @JsonProperty("profile_picture_url") String profilePictureUrl
) {}
