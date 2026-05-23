package com.creatorengine.ai.provider;

import com.creatorengine.ai.dto.GenerateMessageRequest;

import java.util.List;

/**
 * Strategy for generating DM message suggestions.
 *
 * <p>Multiple implementations live side-by-side: an LLM-backed one
 * (e.g. {@code OpenAiMessageProvider}) and an always-on fallback that
 * produces template-based suggestions. {@code AiMessageService} runs
 * through them in {@link #priority()} order, skipping any that report
 * themselves unavailable and falling back on exceptions.</p>
 *
 * <p>Add a new provider (Anthropic, Cohere, etc.) by implementing this
 * interface and registering it as a Spring bean — the service picks it
 * up automatically via constructor injection.</p>
 */
public interface AiMessageProvider {

    /** Stable identifier returned in the response payload. */
    String name();

    /**
     * Lower runs first. The fallback uses a high number so it sits
     * last in the chain; LLM providers should sit below 100.
     */
    int priority();

    /**
     * Whether this provider can serve a request right now. The OpenAI
     * provider returns false if its API key isn't configured, so the
     * service skips it without trying.
     */
    boolean isAvailable();

    /**
     * Generate exactly 3 suggestions. Implementations should throw on
     * any failure (network, parse, rate-limit) — the service catches
     * and tries the next provider.
     */
    List<String> generateSuggestions(GenerateMessageRequest req) throws Exception;
}
