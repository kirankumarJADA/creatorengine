package com.creatorengine.aifaq.service;

import com.creatorengine.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Google's Gemini generateContent REST API.
 * Used by AiFaqService to answer DMs that fall through keyword automations.
 *
 * Endpoint: POST {baseUrl}/models/{model}:generateContent?key={apiKey}
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final AppProperties.Ai.Gemini config;
    private final ObjectMapper json;
    private RestClient restClient;

    public GeminiClient(AppProperties props, ObjectMapper json) {
        this.config = props.getAi().getGemini();
        this.json = json;
    }

    public boolean isAvailable() {
        return StringUtils.hasText(config.getApiKey());
    }

    /**
     * Ask Gemini a question with the given system instruction as context.
     * Returns the model's plain text answer, or empty if the call fails.
     */
    public String generateAnswer(String systemInstruction, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key not configured.");
        }

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 300
                )
        );

        String responseBody = client().post()
                .uri(uri -> uri
                        .path("/models/" + config.getModel() + ":generateContent")
                        .queryParam("key", config.getApiKey())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractText(responseBody);
    }

    private String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty body.");
        }

        try {
            JsonNode root = json.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode() || !textNode.isTextual()) {
                throw new IllegalStateException("Gemini response missing candidates[0].content.parts[0].text");
            }

            return textNode.asText().trim();
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini response: {}", ex.getMessage());
            throw new IllegalStateException("Failed to parse Gemini response: " + ex.getMessage(), ex);
        }
    }

    private RestClient client() {
        if (restClient == null) {
            int timeoutMs = config.getTimeoutMs() > 0 ? config.getTimeoutMs() : 20000;
            restClient = RestClient.builder()
                    .baseUrl(config.getBaseUrl())
                    .requestFactory(new SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(Duration.ofMillis(10_000));
                        setReadTimeout(Duration.ofMillis(timeoutMs));
                    }})
                    .build();
        }
        return restClient;
    }
}
