package com.creatorengine.automation.service;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Automation business logic — what the controller delegates to.
 *
 * <p>Owner ownership is implicit because the repository methods take
 * the uid directly and write to the per-user subcollection. There's
 * no separate {@code where userId = ?} guard to remember.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final AutomationRepository repository;

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

        // toEntity() handles both shapes — legacy single-action OR new
        // multi-step chain. We pull the normalised fields off it and
        // patch the existing row so runtime counters are preserved.
        Automation incoming = req.toEntity();

        existing.setName(req.name() == null || req.name().isBlank()
                ? existing.getName()
                : req.name().trim());
        existing.setTrigger(req.trigger());
        existing.setCondition(req.condition().toEntity());

        // Chain vs legacy update: mirror exactly what toEntity decided.
        // When the request carried a chain, we also clear the legacy
        // fields on the existing row so they don't drift back into play.
        existing.setAction(incoming.getAction());
        existing.setMessage(incoming.getMessage());
        existing.setActions(incoming.getActions());

        if (req.enabled() != null) existing.setEnabled(req.enabled());
        if (req.cooldownMinutes() != null) {
            existing.setCooldownMinutes(Math.max(0, Math.min(req.cooldownMinutes(), 24 * 60)));
        }

        Automation saved = repository.save(uid, existing);
        log.info("Updated automation id={} for uid={}", id, uid);
        return AutomationResponse.from(saved);
    }

    public void delete(String uid, String id) {
        // Verify ownership before deleting so we 404 instead of silently
        // succeeding when the doc isn't there.
        loadOrThrow(uid, id);
        repository.deleteById(uid, id);
        log.info("Deleted automation id={} for uid={}", id, uid);
    }

    public AutomationResponse toggle(String uid, String id, boolean enabled) {
        Automation existing = loadOrThrow(uid, id);
        existing.setEnabled(enabled);
        return AutomationResponse.from(repository.save(uid, existing));
    }

    // ─── Helpers ─────────────────────────────────────────────
    private Automation loadOrThrow(String uid, String id) {
        return repository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation", id));
    }

    /** Generate a fallback name like "COMMENT → SEND_DM" when none is supplied. */
    private String deriveName(Automation a) {
        // First action of the effective chain — works for legacy single-action
        // and new multi-step automations alike.
        ActionType actionType = a.getEffectiveActions().stream()
                .findFirst()
                .map(Automation.Action::getType)
                .orElse(null);
        return "%s → %s".formatted(
                a.getTrigger() != null ? a.getTrigger().name() : "TRIGGER",
                actionType != null ? actionType.name() : "ACTION"
        );
    }
}
