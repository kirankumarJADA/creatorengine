package com.creatorengine.instagram.service;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Account-state CRUD — a thin layer above the repository that exists
 * so the controllers and OAuth service don't talk to Firestore directly.
 * Keeps the seam well-defined for testing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramAccountService {

    private final InstagramAccountRepository repository;

    public Optional<InstagramAccount> find(String uid) {
        return repository.findByUid(uid);
    }

    /**
     * Persist (or replace) the connected account for a user. Always
     * stamps {@code connectedAt} on the first save and {@code lastSyncAt}
     * on every save.
     */
    public InstagramAccount save(String uid, InstagramAccount account) {
        Instant now = Instant.now();
        if (account.getConnectedAt() == null) account.setConnectedAt(now);
        account.setLastSyncAt(now);
        account.setConnected(true);
        InstagramAccount saved = repository.save(uid, account);
        log.info("Saved Instagram account uid={} ig={}", uid, saved.getInstagramUserId());
        return saved;
    }

    /**
     * Update lastSyncAt — called when a webhook event arrives for this
     * account so the UI can show "last activity 2 minutes ago".
     */
    public void touchLastSync(String uid) {
        repository.findByUid(uid).ifPresent(a -> {
            a.setLastSyncAt(Instant.now());
            repository.save(uid, a);
        });
    }

    public void disconnect(String uid) {
        repository.deleteByUid(uid);
        log.info("Disconnected Instagram account uid={}", uid);
    }

    /** Reverse lookup — used by the webhook handler to attribute events. */
    public Optional<InstagramAccountRepository.OwnedAccount> findByInstagramUserId(String igId) {
        return repository.findByInstagramUserId(igId);
    }
}
