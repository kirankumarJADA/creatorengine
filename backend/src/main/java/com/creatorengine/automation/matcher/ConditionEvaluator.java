package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.MatchType;
import com.creatorengine.instagram.dto.WebhookEventDto;
import org.springframework.stereotype.Component;

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
        String needle = norm(condition.getKeyword());

        // Use message text as the primary haystack.
        // For ice breaker button taps, Instagram sends the button payload in
        // quick_reply.payload and may leave message.text empty — fall back to it.
        String rawMessage = event != null ? event.message() : null;
        if ((rawMessage == null || rawMessage.isBlank())
                && event != null
                && event.quickReplyPayload() != null
                && !event.quickReplyPayload().startsWith("fgate:")) {
            rawMessage = event.quickReplyPayload();
        }
        String haystack = norm(rawMessage);

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
            case EXACT:
                return haystack.equals(needle)
                        ? Result.matched("Exact match on \"" + needle + "\".")
                        : Result.notMatched("Exact match required.");
            case CONTAINS:
                return haystack.contains(needle)
                        ? Result.matched("Keyword \"" + needle + "\" found in content.")
                        : Result.notMatched("Content does not contain \"" + needle + "\".");
            default:
                return Result.notMatched("Unknown match type: " + type);
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().trim();
    }

    public record Result(boolean matched, String reason) {
        public static Result matched(String reason) {
            return new Result(true, reason);
        }

        public static Result notMatched(String reason) {
            return new Result(false, reason);
        }
    }
}