package com.creatorengine.automation.service;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);

    private final AutomationRepository repository;

    public AutomationService(AutomationRepository repository) {
        this.repository = repository;
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

        Automation saved = repository.save(uid, entity);
        log.info("Created automation id={} for uid={}", saved.getId(), uid);
        return AutomationResponse.from(saved);
    }

    public AutomationResponse update(String uid, String id, AutomationRequest req) {
        req.validate();

        Automation existing = loadOrThrow(uid, id);
        Automation incoming = req.toEntity();

        existing.setName(req.name() == null || req.name().isBlank()
                ? existing.getName()
                : req.name().trim());
        existing.setTrigger(req.trigger());
        existing.setCondition(req.condition().toEntity());
        existing.setAction(incoming.getAction());
        existing.setMessage(incoming.getMessage());
        existing.setActions(incoming.getActions());

        if (req.enabled() != null) {
            existing.setEnabled(req.enabled());
        }

        if (req.cooldownMinutes() != null) {
            existing.setCooldownMinutes(Math.max(0, Math.min(req.cooldownMinutes(), 24 * 60)));
        }

        Automation saved = repository.save(uid, existing);
        log.info("Updated automation id={} for uid={}", id, uid);
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