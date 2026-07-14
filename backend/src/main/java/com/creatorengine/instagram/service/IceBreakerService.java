package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.instagram.entity.InstagramAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Instagram Ice Breakers — quick-reply buttons shown in the DM inbox
 * when a user opens a conversation for the first time. When tapped, they send
 * that text as a DM which can then trigger a DM keyword automation.
 *
 * Uses the Instagram Graph API messenger_profile endpoint.
 */
@Service
public class IceBreakerService {

    private static final Logger log = LoggerFactory.getLogger(IceBreakerService.class);

    private final RestClient graphClient;

    public IceBreakerService(AppProperties props) {
        this.graphClient = RestClient.builder()
                .baseUrl("https://graph.instagram.com/" + props.getMeta().getGraphApiVersion())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public record IceBreakerQuestion(String title, String payload) {}

    /**
     * Fetch current ice breakers from Instagram for the given account.
     */
    @SuppressWarnings("unchecked")
    public List<IceBreakerQuestion> fetch(InstagramAccount account) {
        try {
            Map<String, Object> response = graphClient.get()
                    .uri(uri -> uri
                            .path("/" + account.getInstagramUserId() + "/messenger_profile")
                            .queryParam("fields", "ice_breakers")
                            .queryParam("platform", "instagram")
                            .queryParam("access_token", account.getAccessToken())
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null) return List.of();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null || data.isEmpty()) return List.of();

            Map<String, Object> first = data.get(0);
            List<Map<String, Object>> iceBreakers = (List<Map<String, Object>>) first.get("ice_breakers");
            if (iceBreakers == null || iceBreakers.isEmpty()) return List.of();

            List<Map<String, Object>> actions =
                    (List<Map<String, Object>>) iceBreakers.get(0).get("call_to_actions");
            if (actions == null) return List.of();

            return actions.stream()
                    .map(a -> new IceBreakerQuestion(
                            String.valueOf(a.getOrDefault("title", "")),
                            String.valueOf(a.getOrDefault("payload", ""))))
                    .filter(q -> !q.title().isBlank())
                    .toList();

        } catch (Exception ex) {
            log.warn("Failed to fetch ice breakers for ig={}: {}", account.getInstagramUserId(), ex.getMessage());
            return List.of();
        }
    }

    /**
     * Push ice breakers to Instagram (replaces all existing ones).
     * Max 4 questions. Empty list clears all ice breakers.
     */
    public void save(InstagramAccount account, List<IceBreakerQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            delete(account);
            return;
        }

        // Instagram caps at 4 ice breakers
        List<IceBreakerQuestion> capped = questions.stream()
                .filter(q -> q.title() != null && !q.title().isBlank())
                .limit(4)
                .toList();

        List<Map<String, Object>> actions = new ArrayList<>();
        for (IceBreakerQuestion q : capped) {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "POSTBACK");
            action.put("title", q.title().length() > 80 ? q.title().substring(0, 80) : q.title());
            // Payload is what gets sent as the DM text when the user taps the button.
            // Default to the title if no separate payload provided.
            String payload = (q.payload() != null && !q.payload().isBlank()) ? q.payload() : q.title();
            action.put("payload", payload);
            actions.add(action);
        }

        Map<String, Object> iceBreaker = Map.of(
                "call_to_actions", actions,
                "locale", "default"
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("platform", "instagram");
        body.put("ice_breakers", List.of(iceBreaker));

        try {
            graphClient.post()
                    .uri(uri -> uri
                            .path("/" + account.getInstagramUserId() + "/messenger_profile")
                            .queryParam("access_token", account.getAccessToken())
                            .build())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("Ice breakers saved ig={} count={}", account.getInstagramUserId(), capped.size());
        } catch (HttpStatusCodeException ex) {
            log.warn("Ice breakers save failed ig={} status={} body={}",
                    account.getInstagramUserId(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Instagram rejected the ice breakers: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Ice breakers save failed ig={}: {}", account.getInstagramUserId(), ex.getMessage());
            throw new RuntimeException("Failed to save ice breakers: " + ex.getMessage());
        }
    }

    /**
     * Clear all ice breakers for the given account.
     */
    public void delete(InstagramAccount account) {
        Map<String, Object> body = Map.of(
                "platform", "instagram",
                "fields", List.of("ice_breakers")
        );

        try {
            graphClient.method(HttpMethod.DELETE)
                    .uri(uri -> uri
                            .path("/" + account.getInstagramUserId() + "/messenger_profile")
                            .queryParam("access_token", account.getAccessToken())
                            .build())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("Ice breakers cleared ig={}", account.getInstagramUserId());
        } catch (Exception ex) {
            log.warn("Ice breakers clear failed ig={}: {}", account.getInstagramUserId(), ex.getMessage());
        }
    }
}
