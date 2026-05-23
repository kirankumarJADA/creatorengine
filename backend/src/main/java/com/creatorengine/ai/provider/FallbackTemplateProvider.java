package com.creatorengine.ai.provider;

import com.creatorengine.ai.MessageTone;
import com.creatorengine.ai.dto.GenerateMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Always-available provider that produces template-based suggestions.
 *
 * <p>Two reasons it exists:</p>
 * <ol>
 *   <li>Developers can use the AI feature locally without configuring
 *       an OpenAI key.</li>
 *   <li>If the LLM provider is down or rate-limited, users still get
 *       three usable starting points instead of an error toast.</li>
 * </ol>
 *
 * <p>The templates are intentionally simple — they're scaffolding, not
 * polished copy. The user will refine after picking one. Each suggestion
 * matches the requested tone label so the modal's "Tone: friendly"
 * badge stays meaningful even on fallback.</p>
 */
@Slf4j
@Component
public class FallbackTemplateProvider implements AiMessageProvider {

    /** Always last. */
    @Override public int priority() { return Integer.MAX_VALUE; }
    @Override public String name() { return "fallback"; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public List<String> generateSuggestions(GenerateMessageRequest req) {
        String goal     = nullToEmpty(req.goal()).trim();
        String audience = nullToEmpty(req.audience()).trim();
        String cta      = req.ctaOrNone();
        boolean hasCta  = !"(none)".equals(cta);

        // Three tone-targeted variants. Each includes {{username}} so
        // the placeholder is wired correctly from the start; the user
        // edits everything else to taste.
        return List.of(
                friendly(goal, audience, cta, hasCta),
                professional(goal, audience, cta, hasCta),
                salesy(goal, audience, cta, hasCta)
        );
    }

    // ─── Variant templates ───────────────────────────────────────
    private String friendly(String goal, String audience, String cta, boolean hasCta) {
        String body = goal.isEmpty()
                ? "Thanks so much for commenting — really appreciate the love!"
                : "Thanks for commenting! Here's the thing about " + goal + " 👇";
        String tail = hasCta ? "\n\n" + cta : "";
        return "Hey {{username}} 👋 " + body + tail;
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
        String tail = hasCta ? " " + cta : " Let me know — happy to share more.";
        return "{{username}}, " + hook + tail;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
