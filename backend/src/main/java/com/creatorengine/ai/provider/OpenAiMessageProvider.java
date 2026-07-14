package com.creatorengine.ai.provider;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiMessageProvider implements AiMessageProvider {

    // NVIDIA's endpoint is OpenAI-compatible, so we reuse the same request shape.
    private static final String BASE_URL = "https://integrate.api.nvidia.com/v1";

    private static final String SYSTEM_PROMPT = """
            You are helping an Instagram CREATOR write short, punchy DM templates that get sent automatically when someone comments on their post.
            Context: this is a creator-to-fan reply, NOT customer service. The goal is to build community, boost engagement, and grow followers.
            Typical use-cases: thanking commenters, inviting them to follow for more, sharing a link, welcoming new fans, hyping a launch.
            Reply with ONLY a JSON object. No markdown, no code fences, no preamble, no explanation.
            Each generated message must:
            - be under 220 characters (short and punchy — Instagram DMs, not emails)
            - include the literal placeholder {{username}} at least once (so it auto-personalises)
            - feel natural and human, like a real creator talking to a fan
            - match the requested tone closely
            - be plain text, no hashtags
            Do NOT write anything that sounds like a customer service apology or order confirmation.
            """;

    private final ObjectMapper json;

    @Value("${NVIDIA_API_KEY:}")
    private String apiKey;

    @Value("${NVIDIA_MODEL:openai/gpt-oss-20b}")
    private String model;

    private RestClient restClient;

    public OpenAiMessageProvider(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String name() {
        return "nvidia";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public List<String> generateSuggestions(GenerateMessageRequest req) throws Exception {
        RestClient client = client();

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.8,
                "max_tokens", 2048,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt(req))
                )
        );

        String responseBody = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseSuggestions(responseBody);
    }

    private String userPrompt(GenerateMessageRequest req) {
        return """
                Generate exactly 3 short Instagram DM templates for a creator auto-replying to post commenters.

                What the DM is about: %s
                Tone:                 %s
                Audience:             %s
                Call to action:       %s

                Rules:
                - Start with {{username}} or use it naturally in the message
                - Sound like a real creator, NOT a brand or customer service rep
                - Be conversational and energetic
                - Keep each under 220 characters

                Return ONLY this JSON shape and nothing else:
                {"suggestions": ["text 1", "text 2", "text 3"]}
                """.formatted(
                req.goal(),
                req.tone().name().toLowerCase(),
                req.audience() != null && !req.audience().isBlank() ? req.audience() : "Instagram followers / fans",
                req.ctaOrNone()
        );
    }

    private List<String> parseSuggestions(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("AI returned an empty body.");
        }

        JsonNode root = json.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || !contentNode.isTextual()) {
            throw new IllegalStateException("AI response missing choices[0].message.content");
        }

        JsonNode innerRoot = json.readTree(extractJsonObject(contentNode.asText()));
        JsonNode arr = innerRoot.has("suggestions") ? innerRoot.path("suggestions") : innerRoot;

        if (!arr.isArray() || arr.isEmpty()) {
            throw new IllegalStateException("AI content did not contain a suggestions array.");
        }

        List<String> out = new ArrayList<>();
        for (JsonNode el : arr) {
            if (el != null && el.isTextual() && !el.asText().isBlank()) {
                out.add(el.asText().trim());
            }
        }

        if (out.isEmpty()) {
            throw new IllegalStateException("AI returned an empty suggestions array.");
        }
        return out;
    }

    /** Models sometimes wrap JSON in ```fences``` or add stray text — pull out the {...} block. */
    private String extractJsonObject(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .requestFactory(new SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(Duration.ofMillis(10_000));
                        setReadTimeout(Duration.ofMillis(60_000));
                    }})
                    .build();
        }
        return restClient;
    }
}