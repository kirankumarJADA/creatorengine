package com.creatorengine.instagram.service;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import com.creatorengine.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence boundary for {@link InstagramAccount}. Access tokens are
 * encrypted at rest with {@link TokenEncryptionService}; callers always see
 * plaintext tokens from this service's accessor methods.
 *
 * <p>Migration note: tokens stored before encryption was introduced are
 * returned as-is by {@code decrypt()} (the legacy-plaintext fallback), and
 * are upgraded to ciphertext automatically the next time they are saved
 * (e.g. by the periodic token-refresh scheduler).</p>
 */
@Service
public class InstagramAccountService {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountService.class);

    private final InstagramAccountRepository repository;
    private final TokenEncryptionService     tokenEncryption;

    public InstagramAccountService(
            InstagramAccountRepository repository,
            TokenEncryptionService tokenEncryption
    ) {
        this.repository      = repository;
        this.tokenEncryption = tokenEncryption;
    }

    public Optional<InstagramAccount> find(String uid) {
        return repository.findByUid(uid).map(this::decryptInPlace);
    }

    public InstagramAccount save(String uid, InstagramAccount account) {
        Instant now = Instant.now();

        if (account.getConnectedAt() == null) {
            account.setConnectedAt(now);
        }
        account.setLastSyncAt(now);
        account.setConnected(true);

        // Encrypt before writing to Firestore. Idempotent — already-encrypted
        // values (e.g. read-then-resaved without rotation) pass through.
        account.setAccessToken(tokenEncryption.encrypt(account.getAccessToken()));

        InstagramAccount saved = repository.save(uid, account);
        log.info("Saved Instagram account uid={} ig={} (token encrypted)", uid, saved.getInstagramUserId());

        // Return plaintext to the caller, consistent with find().
        return decryptInPlace(saved);
    }

    public void touchLastSync(String uid) {
        repository.findByUid(uid).ifPresent(a -> {
            a.setLastSyncAt(Instant.now());
            // a.accessToken is still the on-disk (encrypted) form here — no
            // re-encryption needed; encrypt() is a no-op on already-encrypted input.
            repository.save(uid, a);
        });
    }

    public void disconnect(String uid) {
        repository.deleteByUid(uid);
        log.info("Disconnected Instagram account uid={}", uid);
    }

    public Optional<InstagramAccountRepository.OwnedAccount> findByInstagramUserId(String igId) {
        return repository.findByInstagramUserId(igId).map(owned -> {
            decryptInPlace(owned.account());
            return owned;
        });
    }

    // ─── internals ─────────────────────────────────

    /** Decrypts the token field in place and returns the same instance. */
    private InstagramAccount decryptInPlace(InstagramAccount account) {
        if (account == null) return null;
        String stored = account.getAccessToken();
        if (stored == null || stored.isEmpty()) return account;
        try {
            account.setAccessToken(tokenEncryption.decrypt(stored));
        } catch (RuntimeException e) {
            log.error("Failed to decrypt access token for ig={}: {}",
                    account.getInstagramUserId(), e.getMessage());
            throw e;
        }
        return account;
    }
}