package com.creatorengine.ai.service;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.creatorengine.ai.dto.GenerateMessageResponse;
import com.creatorengine.ai.provider.AiMessageProvider;
import com.creatorengine.ai.provider.FallbackTemplateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AiMessageService {

    private static final Logger log = LoggerFactory.getLogger(AiMessageService.class);

    private static final int TARGET_COUNT = 3;
    private static final int MAX_LENGTH = 500;

    private final List<AiMessageProvider> ordered;
    private final FallbackTemplateProvider fallback;

    public AiMessageService(List<AiMessageProvider> providers, FallbackTemplateProvider fallback) {
        this.ordered = providers.stream()
                .sorted(Comparator.comparingInt(AiMessageProvider::priority))
                .toList();
        this.fallback = fallback;
    }

    public GenerateMessageResponse generate(GenerateMessageRequest req) {
        for (AiMessageProvider provider : ordered) {
            if (!provider.isAvailable()) {
                log.debug("AI provider '{}' skipped - not available", provider.name());
                continue;
            }

            try {
                List<String> raw = provider.generateSuggestions(req);
                List<String> shaped = shape(raw);

                if (shaped.size() < TARGET_COUNT) {
                    shaped = topUp(shaped, req);
                }

                log.info("AI suggestions delivered by provider='{}'", provider.name());
                return new GenerateMessageResponse(shaped, provider.name());
            } catch (Exception ex) {
                log.warn("AI provider '{}' failed, falling through: {}",
                        provider.name(), ex.getMessage());
            }
        }

        log.error("All AI providers failed, returning empty list.");
        return new GenerateMessageResponse(List.of(), "none");
    }

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
                if (merged.size() >= TARGET_COUNT) {
                    break;
                }
                merged.add(s);
            }

            return List.copyOf(merged).subList(0, Math.min(TARGET_COUNT, merged.size()));
        } catch (Exception e) {
            return partial;
        }
    }
}