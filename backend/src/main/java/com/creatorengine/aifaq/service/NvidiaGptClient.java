package com.creatorengine.aifaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around NVIDIA's hosted, OpenAI-compatible chat completions
 * endpoint (serves open GPT models like gpt-oss-20b for free). Used by
 * AiFaqService to answer DMs that fall through keyword automations.
 *
 * Reuses the same NVIDIA_API_KEY already configured for AI message
 * suggestions (see OpenAiMessageProvider) — no separate billing needed.
 */
@Component
public class NvidiaGptClient {

    private static final Logger log = LoggerFactory.getLogger(NvidiaGptClient.class);
    private static final String BASE_URL = "https://integrate.api.nvidia.com/v1";

    private final ObjectMapper json;

    @Value("${NVIDIA_API_KEY:}")
    private String apiKey;

    @Value("${NVIDIA_MODEL:openai/gpt-oss-20b}")
    private String model;

    private RestClient restClient;

    public NvidiaGptClient(ObjectMapper json) {
        this.json = json;
    }

    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * Ask the model a question with the given system instruction as context.
     * Returns the model's plain text answer.
     */
    public String generateAnswer(String systemInstruction, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI API key not configured.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "max_tokens", 300,
                "messages", List.of(
                        Map.of("role", "system", "content", systemInstruction),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String responseBody = client().post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractText(responseBody);
    }

    private String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("AI returned an empty body.");
        }

        try {
            JsonNode root = json.readTree(responseBody);
            JsonNode textNode = root.path("choices").path(0).path("message").path("content");

            if (textNode.isMissingNode() || !textNode.isTextual()) {
                throw new IllegalStateException("AI response missing choices[0].message.content");
            }

            return textNode.asText().trim();
        } catch (Exception ex) {
            log.warn("Failed to parse AI response: {}", ex.getMessage());
            throw new IllegalStateException("Failed to parse AI response: " + ex.getMessage(), ex);
        }
    }

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .requestFactory(new SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(Duration.ofMillis(10_000));
                        setReadTimeout(Duration.ofMillis(20_000));
                    }})
                    .build();
        }
        return restClient;
    }
}
