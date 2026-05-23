package com.creatorengine.automation.executor;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {{variable}} placeholders in an automation's message
 * template.
 *
 * <p>The frontend ships an equivalent implementation in
 * {@code utils/automationEngine.js} — they MUST stay in lockstep,
 * because the user composes the template with the JS preview in
 * mind and expects the same output at execution time.</p>
 *
 * <p>Unknown variables are left intact ({@code {{foo}}} stays
 * {@code {{foo}}}) — the convention is "broken placeholder = visible
 * bug", not "broken placeholder = silently delete content".</p>
 */
@Component
public class TemplateRenderer {

    private static final Pattern VARIABLE = Pattern.compile(
            "\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    public String render(String template, Map<String, String> vars) {
        if (template == null || template.isEmpty()) return "";
        if (vars == null || vars.isEmpty()) return template;

        Matcher m = VARIABLE.matcher(template);
        StringBuilder sb = new StringBuilder(template.length());
        while (m.find()) {
            String name = m.group(1);
            String replacement = vars.get(name);
            // Leave the placeholder untouched if we don't have a value —
            // it's easier to spot misspelled variables that way.
            m.appendReplacement(
                    sb,
                    Matcher.quoteReplacement(replacement != null ? replacement : m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Convenience for the most common case (just a username). */
    public String renderWithUsername(String template, String username) {
        return render(template, Map.of(
                "username", username != null ? username : "follower"
        ));
    }
}
