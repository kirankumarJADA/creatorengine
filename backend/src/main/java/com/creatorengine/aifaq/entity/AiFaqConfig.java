package com.creatorengine.aifaq.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Per-Instagram-account AI FAQ configuration.
 * Stored at: users/{uid}/accounts/{igAccountId}/aiFaq/config
 *
 * When enabled, any incoming DM that doesn't match a keyword automation
 * falls through to AI, which answers using qaPairs first (closest
 * match) and knowledgeBase as general context. Pro/Agency plan only.
 */
public class AiFaqConfig {

    private boolean enabled = false;
    private String knowledgeBase = "";
    private List<QaPair> qaPairs = new ArrayList<>();
    private Date updatedAt;

    public AiFaqConfig() {}

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(String knowledgeBase) { this.knowledgeBase = knowledgeBase; }

    public List<QaPair> getQaPairs() { return qaPairs; }
    public void setQaPairs(List<QaPair> qaPairs) { this.qaPairs = qaPairs; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /** True if there's anything at all for AI to answer from. */
    public boolean hasContent() {
        boolean hasKb = knowledgeBase != null && !knowledgeBase.isBlank();
        boolean hasQa = qaPairs != null && !qaPairs.isEmpty();
        return hasKb || hasQa;
    }
}
