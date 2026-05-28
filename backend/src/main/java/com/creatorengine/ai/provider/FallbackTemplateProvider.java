package com.creatorengine.ai.provider;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FallbackTemplateProvider implements AiMessageProvider {

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String name() {
        return "fallback";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<String> generateSuggestions(GenerateMessageRequest req) {
        String goal = nullToEmpty(req.goal()).trim();
        String audience = nullToEmpty(req.audience()).trim();
        String cta = req.ctaOrNone();
        boolean hasCta = !"(none)".equals(cta);

        return List.of(
                friendly(goal, audience, cta, hasCta),
                professional(goal, audience, cta, hasCta),
                salesy(goal, audience, cta, hasCta)
        );
    }

    private String friendly(String goal, String audience, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Thanks so much for commenting - really appreciate the love!"
                : "Thanks for commenting! Here's the thing about " + goal + ":";
        String tail = hasCta ? "\n\n" + cta : "";
        return "Hey {{username}}, " + body + tail;
    }

    private String professional(String goal, String audience, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Thanks for reaching out."
                : "Thanks for the comment. Sharing what you asked about (" + goal + "):";
        String tail = hasCta ? "\n\n" + cta : "";
        return "Hi {{username}}, " + body + tail;
    }

    private String salesy(String goal, String audience, String cta, boolean hasCta) {
        String hook = goal.isEmpty()
                ? "ready to take the next step?"
                : "ready to " + goal + "?";
        String tail = hasCta ? " " + cta : " Let me know - happy to share more.";
        return "{{username}}, " + hook + tail;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}