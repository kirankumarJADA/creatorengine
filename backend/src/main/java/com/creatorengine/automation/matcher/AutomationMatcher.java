package com.creatorengine.automation.matcher;

import com.creatorengine.automation.entity.Automation;
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
        if (a.getTrigger() == null) {
            return false;
        }
        return a.getTrigger().name().equals(eventType.name());
    }

    /**
     * Post targeting. When an automation sets a targetPostId it only fires on
     * comments on that one post. A null/blank targetPostId means "any post".
     * Events with no post context (e.g. DMs) skip this filter, so the field is
     * effectively ignored for DM triggers.
     */
    private boolean matchesTargetPost(Automation a, WebhookEventDto event) {
        String target = a.getTargetPostId();
        if (target == null || target.isBlank()) {
            return true;
        }
        String eventPostId = event.postId();
        if (eventPostId == null) {
            return true;
        }
        return eventPostId.equals(target);
    }
}