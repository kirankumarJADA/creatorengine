package com.creatorengine.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MetaMediaResponse(List<MediaItem> data) {
    public record MediaItem(
            String id,
            String caption,
            @JsonProperty("media_type") String mediaType,
            @JsonProperty("media_url") String mediaUrl,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            String permalink,
            String timestamp
    ) {}
}