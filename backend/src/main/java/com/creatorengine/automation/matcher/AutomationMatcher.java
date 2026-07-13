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

        // MULTI-ACCOUNT FIX: use receivingAccountId to scope automations to the
        // correct Instagram account. Previously used findAllByOwner(uid) which
        // returned automations from ALL accounts - causing cross-account firing.
        String igAccountId = event.receivingAccountId();

        List<Automation> all;
        if (igAccountId != null && !igAccountId.isBlank()) {
            all = automationRepository.findAllByOwner(uid, igAccountId);
        } else {
            // Fallback to legacy path if no account ID in event (shouldn't happen)
            log.warn("findCandidates: no receivingAccountId in event uid={} type={} - falling back to legacy",
                    uid, event.type());
            all = automationRepository.findAllByOwner(uid);
        }

        List<Automation> filtered = all.stream()
                .filter(Automation::getEnabled)
                .filter(a -> matchesTriggerType(a, expectedTrigger))
                .filter(a -> matchesTargetPost(a, event))
                .toList();

        log.debug("Matcher uid={} igAccountId={} event={} postId={} candidates={} total={}",
                uid, igAccountId, expectedTrigger, event.postId(), filtered.size(), all.size());

        return filtered;
    }

    private boolean matchesTriggerType(Automation a, EventType eventType) {
        if (a.getTrigger() == null) return false;
        if (a.getTrigger() == TriggerType.NEXT_POST) {
            return eventType == EventType.COMMENT;
        }
        return a.getTrigger().name().equals(eventType.name());
    }

    private boolean matchesTargetPost(Automation a, WebhookEventDto event) {
        String eventPostId = event.postId();
        if (eventPostId == null) return true;

        PostTargetMode mode = a.getEffectiveTargetPostMode();
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