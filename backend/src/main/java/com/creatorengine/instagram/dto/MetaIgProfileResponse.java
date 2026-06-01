package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of graph.instagram.com/me response for Instagram Business Login.
 */
public record MetaIgProfileResponse(
        @JsonProperty("id")                  String id,
        @JsonProperty("user_id")             String userId,
        @JsonProperty("username")            String username,
        @JsonProperty("name")                String name,
        @JsonProperty("profile_picture_url") String profilePictureUrl
) {}