package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.PostTargetMode;
import com.creatorengine.automation.entity.TriggerType;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutomationMatcher {

    private static final Logger log = LoggerFactory.getLogger(AutomationMatcher.class);

    private final AutomationRepository automationRepository;

    public AutomationMatcher(AutomationRepository automationRepository) {
        this.automationRepository = automationRepository;
    }

    public List<Automation> findCandidates(String uid, WebhookEventDto event) {
        if (uid == null || event == null || event.type() == null) {
            return List.of();
        }

        EventType expectedTrigger = event.type();

        List<Automation> all = automationRepository.findAllByOwner(uid);
        List<Automation> filtered = all.stream()
                .filter(Automation::getEnabled)
                .filter(a -> matchesTriggerType(a, expectedTrigger))
                .filter(a -> matchesTargetPost(a, event))
                .toList();

        log.debug("Matcher uid={} event={} postId={} candidates={} total={}",
                uid, expectedTrigger, event.postId(), filtered.size(), all.size());

        return filtered;
    }

    /**
     * NEXT_POST is a UX-level trigger that maps to COMMENT events internally,
     * scoped to the auto-locked post id. All other triggers map 1:1 to their
     * event types.
     */
    private boolean matchesTriggerType(Automation a, EventType eventType) {
        if (a.getTrigger() == null) return false;
        if (a.getTrigger() == TriggerType.NEXT_POST) {
            return eventType == EventType.COMMENT;
        }
        return a.getTrigger().name().equals(eventType.name());
    }

    /**
     * Post targeting:
     *  - DM events have no post context, so this filter is a no-op for them.
     *  - ALL → always matches.
     *  - SPECIFIC → must equal the event's postId.
     *  - NEXT_POST (mode) → if not locked yet (targetPostId blank) → never fires;
     *    once locked, it behaves like SPECIFIC.
     *  - NEXT_POST trigger always implies NEXT_POST mode, same rule applies.
     */
    private boolean matchesTargetPost(Automation a, WebhookEventDto event) {
        String eventPostId = event.postId();
        if (eventPostId == null) return true;

        PostTargetMode mode = a.getEffectiveTargetPostMode();

        // NEXT_POST trigger always means "only the locked post".
        boolean isNextPostTrigger = a.getTrigger() == TriggerType.NEXT_POST;

        if (mode == PostTargetMode.ALL && !isNextPostTrigger) return true;

        if ((mode == PostTargetMode.NEXT_POST || isNextPostTrigger)
                && (a.getTargetPostId() == null || a.getTargetPostId().isBlank())) {
            return false;
        }

        String target = a.getTargetPostId();
        if (target == null || target.isBlank()) return true;
        return eventPostId.equals(target);
    }
}