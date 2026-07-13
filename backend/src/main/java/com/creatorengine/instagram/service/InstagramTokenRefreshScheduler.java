package com.creatorengine.instagram.service;

import com.creatorengine.instagram.dto.MetaTokenResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import com.creatorengine.instagram.repository.InstagramAccountRepository.OwnedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Keeps connected Instagram accounts alive.
 *
 * Long-lived IG tokens last ~60 days. This job refreshes any account whose
 * token has {@code REFRESH_WINDOW} or less remaining, giving it another ~60
 * days. Running ~1 min after startup means it also fires whenever the server
 * wakes/redeploys — useful on hosting that sleeps.
 */
@Component
public class InstagramTokenRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(InstagramTokenRefreshScheduler.class);

    /** Refresh once a token has 10 days or less left. */
    private static final Duration REFRESH_WINDOW = Duration.ofDays(10);

    private final InstagramAccountRepository accountRepository;
    private final InstagramApiClient apiClient;

    public InstagramTokenRefreshScheduler(
            InstagramAccountRepository accountRepository,
            InstagramApiClient apiClient
    ) {
        this.accountRepository = accountRepository;
        this.apiClient = apiClient;
    }

    // ~1 minute after startup, then every 12 hours.
    @Scheduled(initialDelay = 60_000, fixedDelay = 43_200_000)
    public void refreshExpiringTokens() {
        List<OwnedAccount> accounts;
        try {
            accounts = accountRepository.findAll();
        } catch (Exception ex) {
            log.error("Token refresh sweep: could not load accounts: {}", ex.getMessage());
            return;
        }

        Instant now = Instant.now();
        Instant threshold = now.plus(REFRESH_WINDOW);
        int refreshed = 0, skipped = 0, failed = 0;

        for (OwnedAccount owned : accounts) {
            InstagramAccount account = owned.account();

            if (account == null || !account.getConnected() || account.getAccessToken() == null) {
                skipped++;
                continue;
            }

            Instant expiresAt = account.getTokenExpiresAt();
            // Not close to expiry yet — leave it.
            if (expiresAt != null && expiresAt.isAfter(threshold)) {
                skipped++;
                continue;
            }

            try {
                MetaTokenResponse refreshedToken =
                        apiClient.refreshLongLivedToken(account.getAccessToken());

                if (refreshedToken == null || refreshedToken.accessToken() == null) {
                    log.warn("Token refresh returned nothing for ig={}", account.getInstagramUserId());
                    failed++;
                    continue;
                }

                account.setAccessToken(refreshedToken.accessToken());
                if (refreshedToken.expiresIn() != null) {
                    account.setTokenExpiresAt(now.plusSeconds(refreshedToken.expiresIn()));
                }
                account.setLastSyncAt(now);

                accountRepository.save(owned.uid(), account);
                refreshed++;
                log.info("Refreshed IG token for ig={} (uid={})",
                        account.getInstagramUserId(), owned.uid());
            } catch (Exception ex) {
                failed++;
                log.error("Failed to refresh IG token for ig={} (uid={}): {}",
                        account.getInstagramUserId(), owned.uid(), ex.getMessage());
            }
        }

        if (refreshed > 0 || failed > 0) {
            log.info("IG token refresh sweep: refreshed={}, skipped={}, failed={}",
                    refreshed, skipped, failed);
        }
    }
}