package com.creatorengine.instagram.service;

import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
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
import java.util.Optional;

/**
 * Persistence boundary for {@link InstagramAccount}. Access tokens are
 * encrypted at rest with {@link TokenEncryptionService}; callers always see
 * plaintext tokens from this service's accessor methods.
 */
@Service
public class InstagramAccountService {

    private static final Logger log = LoggerFactory.getLogger(InstagramAccountService.class);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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

        account.setAccessToken(tokenEncryption.encrypt(account.getAccessToken()));

        InstagramAccount saved = repository.save(uid, account);
        log.info("Saved Instagram account uid={} ig={} (token encrypted)", uid, saved.getInstagramUserId());

        return decryptInPlace(saved);
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
        return repository.findByInstagramUserId(igId).map(owned -> {
            decryptInPlace(owned.account());
            return owned;
        });
    }

    /**
     * Live-checks a token against Instagram. Returns {@code true} ONLY when
     * Instagram explicitly rejects the token (revoked / expired / invalid).
     * Network errors, timeouts, bad-field errors, or 5xx all return
     * {@code false}, so we never falsely report a working account as down.
     */
    public boolean isTokenRevoked(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return true;
        }
        try {
            String url = "https://graph.instagram.com/me?fields=username&access_token="
                    + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                return false; // token works
            }

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

            // Some other error (rate limit, server issue) — don't disconnect.
            return false;
        } catch (Exception e) {
            log.warn("Token live-check failed (treating as still connected): {}", e.getMessage());
            return false;
        }
    }

    // ─── internals ─────────────────────────────────

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