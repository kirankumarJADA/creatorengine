package com.creatorengine.aifaq.dto;

import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.creatorengine.aifaq.entity.QaPair;

import java.util.List;

/**
 * "Test AI" request — the creator's DRAFT (possibly unsaved) knowledge base
 * and Q&A pairs, plus the test question to try against them.
 */
public record AiFaqTestRequest(
        String knowledgeBase,
        List<QaPair> qaPairs,
        String message
) {
    public AiFaqConfig toDraftConfig() {
        AiFaqConfig config = new AiFaqConfig();
        config.setEnabled(true);
        config.setKnowledgeBase(knowledgeBase);
        config.setQaPairs(qaPairs);
        return config;
    }
}
