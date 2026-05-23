package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ConditionType;
import com.creatorengine.automation.entity.MatchType;
import com.creatorengine.instagram.dto.WebhookEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pure-function condition matcher.
 *
 * <p>Stateless, side-effect-free — safe to share, safe to test.
 * Mirrors the JS implementation in
 * {@code frontend/src/utils/automationEngine.js} so the wizard
 * preview and the live execution agree on what "matches".</p>
 */
@Slf4j
@Component
public class ConditionEvaluator {

    public Result evaluate(Automation.Condition condition, WebhookEventDto event) {
        if (condition == null || condition.getType() == null) {
            return Result.matched("Default ANY match.");
        }

        switch (condition.getType()) {
            case ANY:
                return Result.matched("Any incoming event matches.");

            case KEYWORD:
                return evaluateKeyword(condition, event);

            default:
                return Result.notMatched("Unknown condition type: " + condition.getType());
        }
    }

    private Result evaluateKeyword(Automation.Condition condition, WebhookEventDto event) {
        String needle   = norm(condition.getKeyword());
        String haystack = norm(event != null ? event.message() : null);

        if (needle.isEmpty()) {
            return Result.notMatched("No keyword configured on automation.");
        }
        if (haystack.isEmpty()) {
            return Result.notMatched("Event content is empty.");
        }

        MatchType type = condition.getMatchType() != null
                ? condition.getMatchType()
                : MatchType.CONTAINS;

        switch (type) {
            case EXACT: {
                boolean ok = haystack.equals(needle);
                return ok
                        ? Result.matched("Exact match on \"" + needle + "\".")
                        : Result.notMatched("Exact match required: \"" + haystack + "\" ≠ \"" + needle + "\".");
            }
            case CONTAINS: {
                boolean ok = haystack.contains(needle);
                return ok
                        ? Result.matched("Keyword \"" + needle + "\" found in content.")
                        : Result.notMatched("Content does not contain \"" + needle + "\".");
            }
            default:
                return Result.notMatched("Unknown match type: " + type);
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().trim();
    }

    /** Tiny value object so callers can pattern-match on outcome + reason. */
    public record Result(boolean matched, String reason) {
        public static Result matched(String reason)    { return new Result(true,  reason); }
        public static Result notMatched(String reason) { return new Result(false, reason); }
    }
}
