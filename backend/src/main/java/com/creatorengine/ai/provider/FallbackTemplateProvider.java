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
        String cta = req.ctaOrNone();
        boolean hasCta = !"(none)".equals(cta);

        return List.of(
                friendly(goal, cta, hasCta),
                casual(goal, cta, hasCta),
                hype(goal, cta, hasCta)
        );
    }

    private String friendly(String goal, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Thanks for commenting, means a lot! ❤️ Follow for more content like this."
                : "Thanks for commenting about " + goal + "! Follow along — dropping more on this soon 🙌";
        String tail = hasCta ? " " + cta : "";
        return "Hey {{username}}! " + body + tail;
    }

    private String casual(String goal, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Love seeing you here! Like & follow so you never miss a post 👀"
                : "Glad you're into " + goal + "! Hit follow — I post about this all the time 🔥";
        String tail = hasCta ? " " + cta : "";
        return "{{username}} " + body + tail;
    }

    private String hype(String goal, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Your comment made my day 🙏 Follow for more — big things coming!"
                : "Thanks for the love on " + goal + "! Way more content on this coming — make sure you're following 🚀";
        String tail = hasCta ? "\n" + cta : "";
        return "{{username}}! " + body + tail;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}