package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.PostTargetMode;
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

    private boolean matchesTriggerType(Automation a, EventType eventType) {
        if (a.getTrigger() == null) return false;
        return a.getTrigger().name().equals(eventType.name());
    }

    /**
     * Post targeting:
     *  - ALL → always matches (DMs etc. ignore this anyway).
     *  - SPECIFIC → must equal the event's postId.
     *  - NEXT_POST → if not locked yet (targetPostId null) → never matches; once locked, behaves like SPECIFIC.
     */
    private boolean matchesTargetPost(Automation a, WebhookEventDto event) {
        PostTargetMode mode = a.getEffectiveTargetPostMode();
        String eventPostId = event.postId();

        // Events without a post context (DMs) bypass post targeting.
        if (eventPostId == null) return true;

        if (mode == PostTargetMode.ALL) return true;

        if (mode == PostTargetMode.NEXT_POST
                && (a.getTargetPostId() == null || a.getTargetPostId().isBlank())) {
            // Still waiting for the user's next upload; don't fire.
            return false;
        }

        String target = a.getTargetPostId();
        if (target == null || target.isBlank()) return true;
        return eventPostId.equals(target);
    }
}