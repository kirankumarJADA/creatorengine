package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Finds the automations that could fire for an event.
 *
 * <p>Filtering happens in two passes:</p>
 * <ol>
 *   <li><b>Cheap pre-filter</b> — pulls the user's automations from
 *       Firestore and keeps only those whose trigger matches the
 *       event type and that are enabled. One read per dispatch.</li>
 *   <li><b>Condition evaluation</b> — done by
 *       {@link ConditionEvaluator}, called by the engine on each
 *       candidate this method returns.</li>
 * </ol>
 *
 * <p>We do not push the trigger filter into the Firestore query —
 * the per-user collection is small (tens to low hundreds) and an
 * in-memory filter avoids needing extra composite indexes.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationMatcher {

    private final AutomationRepository automationRepository;

    public List<Automation> findCandidates(String uid, WebhookEventDto event) {
        if (uid == null || event == null || event.type() == null) {
            return List.of();
        }
        EventType expectedTrigger = event.type();

        List<Automation> all = automationRepository.findAllByOwner(uid);
        List<Automation> filtered = all.stream()
                .filter(Automation::isEnabled)
                .filter(a -> matchesTriggerType(a, expectedTrigger))
                .toList();

        log.debug("Matcher uid={} event={} → {} candidate(s) (of {} total)",
                uid, expectedTrigger, filtered.size(), all.size());
        return filtered;
    }

    /**
     * Trigger types live in two different enum spaces — automation's
     * {@code TriggerType} (COMMENT / DM / STORY_REPLY) and the webhook
     * event's {@code EventType} (same names). We compare by name so a
     * future divergence (e.g. STORY_MENTION) doesn't break things
     * silently — a mismatch just stops matching.
     */
    private boolean matchesTriggerType(Automation a, EventType eventType) {
        if (a.getTrigger() == null) return false;
        return a.getTrigger().name().equals(eventType.name());
    }
}
