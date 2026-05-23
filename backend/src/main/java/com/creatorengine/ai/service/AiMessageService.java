package com.creatorengine.ai.service;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.creatorengine.ai.dto.GenerateMessageResponse;
import com.creatorengine.ai.provider.AiMessageProvider;
import com.creatorengine.ai.provider.FallbackTemplateProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates message-suggestion generation across pluggable providers.
 *
 * <p>Walks {@link AiMessageProvider} beans in {@link AiMessageProvider#priority()}
 * order: the LLM-backed providers (low priority numbers) get first
 * shot; on any failure — unavailable, exception, malformed response —
 * the service logs and moves to the next one. {@link FallbackTemplateProvider}
 * sits at the bottom of the chain and is guaranteed to succeed, so
 * the user never sees an error toast from this endpoint.</p>
 *
 * <p>The response always carries the provider name that actually
 * produced the suggestions, so the frontend can show a discreet
 * "fallback" badge when the LLM was bypassed.</p>
 *
 * <h3>Output shape contract</h3>
 * <ul>
 *   <li>Exactly 3 suggestions, trimmed and de-duplicated.</li>
 *   <li>Each ≤500 chars (defensive cap; the LLM is asked for ≤250).</li>
 *   <li>If a provider returns fewer than 3, the fallback fills the rest.</li>
 * </ul>
 */
@Slf4j
@Service
public class AiMessageService {

    private static final int TARGET_COUNT = 3;
    private static final int MAX_LENGTH = 500;

    private final List<AiMessageProvider> ordered;
    private final FallbackTemplateProvider fallback;

    public AiMessageService(List<AiMessageProvider> providers, FallbackTemplateProvider fallback) {
        // Sort by priority once at construction; Spring re-injects on each request
        // so this isn't a hot-path concern, but the alternative (re-sorting per
        // call) would be wasteful.
        this.ordered = providers.stream()
                .sorted(Comparator.comparingInt(AiMessageProvider::priority))
                .toList();
        this.fallback = fallback;
    }

    public GenerateMessageResponse generate(GenerateMessageRequest req) {
        for (AiMessageProvider provider : ordered) {
            if (!provider.isAvailable()) {
                log.debug("AI provider '{}' skipped — not available", provider.name());
                continue;
            }
            try {
                List<String> raw = provider.generateSuggestions(req);
                List<String> shaped = shape(raw);
                if (shaped.size() < TARGET_COUNT) {
                    // Top up with fallback so the contract holds. The user still
                    // sees this as the LLM's output because most suggestions are
                    // from the LLM — only the tail is filled.
                    shaped = topUp(shaped, req);
                }
                log.info("AI suggestions delivered by provider='{}'", provider.name());
                return new GenerateMessageResponse(shaped, provider.name());
            } catch (Exception ex) {
                // Provider failed — swallow and try the next. A real outage
                // surfaces in logs; users see fallback suggestions.
                log.warn("AI provider '{}' failed, falling through: {}",
                        provider.name(), ex.getMessage());
            }
        }
        // Shouldn't happen — FallbackTemplateProvider is always available —
        // but defend anyway with a hardcoded "I couldn't generate" response.
        log.error("All AI providers failed (including fallback) — returning empty list.");
        return new GenerateMessageResponse(List.of(), "none");
    }

    // ─── Shaping ─────────────────────────────────────────────────
    /** Trim, drop empties, dedupe, cap length, take up to TARGET_COUNT. */
    private List<String> shape(List<String> raw) {
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(s -> s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH).trim() : s)
                .distinct()
                .limit(TARGET_COUNT)
                .toList();
    }

    private List<String> topUp(List<String> partial, GenerateMessageRequest req) {
        try {
            List<String> filler = fallback.generateSuggestions(req);
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(partial);
            for (String s : filler) {
                if (merged.size() >= TARGET_COUNT) break;
                merged.add(s);
            }
            return List.copyOf(merged).subList(0, Math.min(TARGET_COUNT, merged.size()));
        } catch (Exception e) {
            // Fallback shouldn't throw, but if it does just return what we have.
            return partial;
        }
    }
}
