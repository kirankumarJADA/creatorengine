package com.creatorengine.ai.dto;

import java.util.List;

/**
 * Output of POST /api/ai/generate-message.
 *
 * @param suggestions exactly 3 DM templates; each ≤250 chars and
 *                    contains the {@code {{username}}} placeholder.
 * @param provider    which provider answered — {@code "openai"} when
 *                    the LLM call succeeded, {@code "fallback"} when
 *                    we degraded to templates. The frontend can show
 *                    a small badge on fallback so the user knows the
 *                    suggestions aren't LLM-quality this time.
 */
public record GenerateMessageResponse(
        List<String> suggestions,
        String provider
) { }
