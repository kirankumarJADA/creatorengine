package com.creatorengine.automation.service;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.PostTargetMode;
import com.creatorengine.automation.entity.TriggerType;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.InstagramApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);

    private final AutomationRepository repository;
    private final InstagramAccountService instagramAccountService;
    private final InstagramApiClient instagramApiClient;

    public AutomationService(
            AutomationRepository repository,
            InstagramAccountService instagramAccountService,
            InstagramApiClient instagramApiClient
    ) {
        this.repository = repository;
        this.instagramAccountService = instagramAccountService;
        this.instagramApiClient = instagramApiClient;
    }

    public List<AutomationResponse> listForUser(String uid) {
        return repository.findAllByOwner(uid).stream()
                .map(AutomationResponse::from)
                .toList();
    }

    public AutomationResponse get(String uid, String id) {
        return AutomationResponse.from(loadOrThrow(uid, id));
    }

    public AutomationResponse create(String uid, AutomationRequest req) {
        req.validate();

        Automation entity = req.toEntity();
        if (entity.getName() == null || entity.getName().isBlank()) {
            entity.setName(deriveName(entity));
        }

        if (entity.getTrigger() == TriggerType.NEXT_POST) {
            entity.setTargetPostMode(PostTargetMode.NEXT_POST);
            entity.setTargetPostId(null);
            entity.setNextPostLockedAt(null);
        }

        if (entity.getEffectiveTargetPostMode() == PostTargetMode.NEXT_POST) {
            entity.setBaselineMediaIds(snapshotMediaIds(uid));
            entity.setTargetPostId(null);
            entity.setNextPostLockedAt(null);
        }

        Automation saved = repository.save(uid, entity);
        log.info("Created automation id={} uid={} trigger={} mode={} baselineSize={}",
                saved.getId(), uid, saved.getTrigger(), saved.getEffectiveTargetPostMode(),
                saved.getBaselineMediaIds() == null ? 0 : saved.getBaselineMediaIds().size());
        return AutomationResponse.from(saved);
    }

    public AutomationResponse update(String uid, String id, AutomationRequest req) {
        req.validate();

        Automation existing = loadOrThrow(uid, id);
        Automation incoming = req.toEntity();

        PostTargetMode prevMode = existing.getEffectiveTargetPostMode();
        TriggerType prevTrigger = existing.getTrigger();

        existing.setName(req.name() == null || req.name().isBlank()
                ? existing.getName()
                : req.name().trim());
        existing.setTrigger(req.trigger());
        existing.setCondition(req.condition().toEntity());
        existing.setAction(incoming.getAction());
        existing.setMessage(incoming.getMessage());
        existing.setActions(incoming.getActions());
        existing.setPublicReplyEnabled(incoming.getPublicReplyEnabled());
        existing.setPublicReplies(incoming.getPublicReplies());
        existing.setFollowGateEnabled(incoming.getFollowGateEnabled());
        existing.setFollowGateMessage(incoming.getFollowGateMessage());
        existing.setFollowGateButtonLabel(incoming.getFollowGateButtonLabel());

        PostTargetMode newMode = incoming.getTargetPostMode();
        if (req.trigger() == TriggerType.NEXT_POST) {
            newMode = PostTargetMode.NEXT_POST;
        }
        existing.setTargetPostMode(newMode);

        if (newMode == PostTargetMode.NEXT_POST) {
            boolean switchedIntoNextPost =
                    prevMode != PostTargetMode.NEXT_POST
                    && prevTrigger != TriggerType.NEXT_POST;
            if (switchedIntoNextPost) {
                existing.setBaselineMediaIds(snapshotMediaIds(uid));
                existing.setTargetPostId(null);
                existing.setNextPostLockedAt(null);
            }
        } else if (newMode == PostTargetMode.SPECIFIC) {
            existing.setTargetPostId(incoming.getTargetPostId());
            existing.setBaselineMediaIds(null);
            existing.setNextPostLockedAt(null);
        } else {
            existing.setTargetPostId(null);
            existing.setBaselineMediaIds(null);
            existing.setNextPostLockedAt(null);
        }

        if (req.enabled() != null) {
            existing.setEnabled(req.enabled());
        }

        if (req.cooldownMinutes() != null) {
            existing.setCooldownMinutes(Math.max(0, Math.min(req.cooldownMinutes(), 24 * 60)));
        }

        Automation saved = repository.save(uid, existing);
        log.info("Updated automation id={} uid={} trigger={} mode={}",
                id, uid, saved.getTrigger(), saved.getEffectiveTargetPostMode());
        return AutomationResponse.from(saved);
    }

    public void delete(String uid, String id) {
        loadOrThrow(uid, id);
        repository.deleteById(uid, id);
        log.info("Deleted automation id={} for uid={}", id, uid);
    }

    public AutomationResponse toggle(String uid, String id, boolean enabled) {
        Automation existing = loadOrThrow(uid, id);
        existing.setEnabled(enabled);
        return AutomationResponse.from(repository.save(uid, existing));
    }

    private Automation loadOrThrow(String uid, String id) {
        return repository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation", id));
    }

    private List<String> snapshotMediaIds(String uid) {
        try {
            Optional<InstagramAccount> account = instagramAccountService.find(uid);
            if (account.isEmpty() || account.get().getAccessToken() == null) {
                log.info("snapshotMediaIds: no IG account for uid={}, using empty baseline", uid);
                return List.of();
            }
            var media = instagramApiClient.fetchMedia(account.get().getAccessToken());
            if (media == null || media.data() == null) return List.of();
            return media.data().stream()
                    .map(m -> m.id())
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("snapshotMediaIds failed for uid={}: {}", uid, e.getMessage());
            return List.of();
        }
    }

    private String deriveName(Automation a) {
        ActionType actionType = a.getEffectiveActions().stream()
                .findFirst()
                .map(Automation.Action::getType)
                .orElse(null);

        return "%s -> %s".formatted(
                a.getTrigger() != null ? a.getTrigger().name() : "TRIGGER",
                actionType != null ? actionType.name() : "ACTION"
        );
    }
}