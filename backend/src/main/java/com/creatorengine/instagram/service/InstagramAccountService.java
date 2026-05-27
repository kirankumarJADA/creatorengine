package com.creatorengine.instagram.service;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class InstagramAccountService {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountService.class);

    private final InstagramAccountRepository repository;

    public InstagramAccountService(InstagramAccountRepository repository) {
        this.repository = repository;
    }

    public Optional<InstagramAccount> find(String uid) {
        return repository.findByUid(uid);
    }

    public InstagramAccount save(String uid, InstagramAccount account) {
        Instant now = Instant.now();

        if (account.getConnectedAt() == null) {
            account.setConnectedAt(now);
        }

        account.setLastSyncAt(now);
        account.setConnected(true);

        InstagramAccount saved = repository.save(uid, account);
        log.info("Saved Instagram account uid={} ig={}", uid, saved.getInstagramUserId());

        return saved;
    }

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

    public Optional<InstagramAccountRepository.OwnedAccount> findByInstagramUserId(String igId) {
        return repository.findByInstagramUserId(igId);
    }
}