package com.creatorengine.aifaq.dto;

import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.creatorengine.aifaq.entity.QaPair;

import java.util.List;

public record AiFaqConfigResponse(
        boolean enabled,
        String knowledgeBase,
        List<QaPair> qaPairs,
        boolean planEligible,
        String plan
) {
    public static AiFaqConfigResponse from(AiFaqConfig config, boolean planEligible, String plan) {
        return new AiFaqConfigResponse(
                config.getEnabled(),
                config.getKnowledgeBase(),
                config.getQaPairs(),
                planEligible,
                plan
        );
    }
}
