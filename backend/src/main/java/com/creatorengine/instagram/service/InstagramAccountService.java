package com.creatorengine.instagram.service;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import com.creatorengine.plan.service.PlanService;
import com.creatorengine.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class InstagramAccountService {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountService.class);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final InstagramAccountRepository repository;
    private final TokenEncryptionService tokenEncryption;
    private final PlanService planService;

    public InstagramAccountService(
            InstagramAccountRepository repository,
            TokenEncryptionService tokenEncryption,
            PlanService planService
    ) {
        this.repository = repository;
        this.tokenEncryption = tokenEncryption;
        this.planService = planService;
    }

    /**
     * Find the first connected account for a user (backward compat for
     * single-account usage). Migrates legacy accounts on first call.
     */
    public Optional<InstagramAccount> find(String uid) {
        migrateIfNeeded(uid);
        return repository.findByUid(uid).map(this::decryptInPlace);
    }

    /**
     * Find a specific account by uid + instagramUserId.
     */
    public Optional<InstagramAccount> findByIgId(String uid, String instagramUserId) {
        return repository.findByUidAndIgId(uid, instagramUserId).map(this::decryptInPlace);
    }

    /**
     * Find ALL connected accounts for a user.
     */
    public List<InstagramAccount> findAll(String uid) {
        migrateIfNeeded(uid);
        return repository.findAllByUid(uid).stream()
                .map(this::decryptInPlace)
                .toList();
    }

    /**
     * Count connected accounts for a user.
     */
    public int countAccounts(String uid) {
        return repository.countByUid(uid);
    }

    /**
     * Save/connect a new Instagram account.
     * Enforces plan limits — throws if the user has hit their account cap.
     */
    public InstagramAccount save(String uid, InstagramAccount account) {
        migrateIfNeeded(uid);

        // Check if this is an existing account being updated (reconnect)
        boolean isExisting = repository.findByUidAndIgId(uid, account.getInstagramUserId()).isPresent();

        if (!isExisting) {
            int currentCount = repository.countByUid(uid);
            if (!planService.canAddAccount(uid, currentCount)) {
                int maxAccounts = planService.maxAccounts(uid);
                throw new com.creatorengine.exception.BadRequestException(
                        "You've reached your plan's limit of " + maxAccounts
                                + " Instagram account" + (maxAccounts == 1 ? "" : "s")
                                + ". Upgrade your plan to connect more accounts.");
            }
        }

        Instant now = Instant.now();
        if (account.getConnectedAt() == null) {
            account.setConnectedAt(now);
        }
        account.setLastSyncAt(now);
        account.setConnected(true);
        account.setAccessToken(tokenEncryption.encrypt(account.getAccessToken()));

        InstagramAccount saved = repository.save(uid, account);
        log.info("Saved Instagram account uid={} ig={} (token encrypted)",
                uid, saved.getInstagramUserId());

        return decryptInPlace(saved);
    }

    public void touchLastSync(String uid) {
        repository.findAllByUid(uid).forEach(a -> {
            a.setLastSyncAt(Instant.now());
            repository.save(uid, a);
        });
    }

    /**
     * Disconnect a specific Instagram account by instagramUserId.
     */
    public void disconnect(String uid, String instagramUserId) {
        repository.deleteByUidAndIgId(uid, instagramUserId);
        log.info("Disconnected Instagram account uid={} ig={}", uid, instagramUserId);
    }

    /**
     * Disconnect all accounts for a user (legacy disconnect behavior).
     */
    public void disconnectAll(String uid) {
        repository.deleteByUid(uid);
        log.info("Disconnected all Instagram accounts uid={}", uid);
    }

    public Optional<InstagramAccountRepository.OwnedAccount> findByInstagramUserId(String igId) {
        return repository.findByInstagramUserId(igId).map(owned -> {
            decryptInPlace(owned.account());
            return owned;
        });
    }

    /**
     * Migrate legacy single-account doc to the new multi-account subcollection.
     * Safe to call on every request — no-ops if already migrated or no legacy data.
     */
    public void migrateIfNeeded(String uid) {
        // Already has accounts in the new collection — migration done
        if (repository.countByUid(uid) > 0) return;

        repository.findLegacy(uid).ifPresent(legacyAccount -> {
            if (legacyAccount.getInstagramUserId() == null) {
                log.warn("Legacy account for uid={} has no instagramUserId - skipping migration", uid);
                return;
            }
            log.info("Migrating legacy Instagram account for uid={} ig={}",
                    uid, legacyAccount.getInstagramUserId());
            repository.save(uid, legacyAccount);
            repository.deleteLegacy(uid);
            log.info("Migration complete for uid={}", uid);
        });
    }

    public boolean isTokenRevoked(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) return true;
        try {
            String url = "https://graph.instagram.com/me?fields=username&access_token="
                    + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) return false;

            String body = resp.body() == null ? "" : resp.body().toLowerCase();
            boolean tokenError =
                    body.contains("oauthexception")
                    || body.contains("code\":190")
                    || body.contains("access token")
                    || body.contains("session has been invalidated")
                    || body.contains("session is invalid")
                    || body.contains("expired");

            if (tokenError) {
                log.info("Token live-check rejected (HTTP {}): {}", resp.statusCode(), resp.body());
                return true;
            }

            return false;
        } catch (Exception e) {
            log.warn("Token live-check failed (treating as still connected): {}", e.getMessage());
            return false;
        }
    }

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