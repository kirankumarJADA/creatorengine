package com.creatorengine.ai.provider;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.creatorengine.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiMessageProvider implements AiMessageProvider {

    private static final String SYSTEM_PROMPT = """
            You are a copywriting assistant helping a creator write short Instagram DM templates for an automated reply.
            Output JSON only. No markdown. No preamble.
            Each generated message must:
            - be under 250 characters
            - include the literal placeholder {{username}} at least once
            - match the requested tone closely
            - be plain text
            """;

    private final AppProperties props;
    private final ObjectMapper json;
    private RestClient restClient;

    public OpenAiMessageProvider(AppProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(props.getAi().getOpenai().getApiKey());
    }

    @Override
    public List<String> generateSuggestions(GenerateMessageRequest req) throws Exception {
        var cfg = props.getAi().getOpenai();
        RestClient client = client(cfg);

        Map<String, Object> body = Map.of(
                "model", cfg.getModel(),
                "temperature", 0.8,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt(req))
                )
        );

        String responseBody = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + cfg.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseSuggestions(responseBody);
    }

    private String userPrompt(GenerateMessageRequest req) {
        return """
                Generate exactly 3 short Instagram DM templates.

                Goal:     %s
                Tone:     %s
                Audience: %s
                CTA:      %s

                Return JSON with this exact shape:
                {"suggestions": ["text 1", "text 2", "text 3"]}
                """.formatted(
                req.goal(),
                req.tone().name().toLowerCase(),
                req.audience(),
                req.ctaOrNone()
        );
    }

    private List<String> parseSuggestions(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty body.");
        }

        JsonNode root = json.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || !contentNode.isTextual()) {
            throw new IllegalStateException("OpenAI response missing choices[0].message.content");
        }

        JsonNode innerRoot = json.readTree(contentNode.asText());
        JsonNode arr = innerRoot.path("suggestions");

        if (!arr.isArray() || arr.isEmpty()) {
            throw new IllegalStateException("OpenAI content did not contain a suggestions array.");
        }

        List<String> out = new ArrayList<>();

        for (JsonNode el : arr) {
            if (el != null && el.isTextual() && !el.asText().isBlank()) {
                out.add(el.asText().trim());
            }
        }

        if (out.isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty suggestions array.");
        }

        return out;
    }

    private RestClient client(AppProperties.Ai.Openai cfg) {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(cfg.getBaseUrl())
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        int t = Math.max(1000, cfg.getTimeoutMs());
                        setConnectTimeout(Duration.ofMillis(Math.min(t, 10_000)));
                        setReadTimeout(Duration.ofMillis(t));
                    }})
                    .build();
        }

        return restClient;
    }
}