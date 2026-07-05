package com.creatorengine.automation.service;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.automation.repository.AutomationRepository.OwnedAutomation;
import com.creatorengine.instagram.dto.MetaMediaResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.InstagramApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NextPostLockerService {

    private static final Logger log = LoggerFactory.getLogger(NextPostLockerService.class);

    private final AutomationRepository automationRepository;
    private final InstagramAccountService instagramAccountService;
    private final InstagramApiClient instagramApiClient;

    public NextPostLockerService(
            AutomationRepository automationRepository,
            InstagramAccountService instagramAccountService,
            InstagramApiClient instagramApiClient
    ) {
        this.automationRepository = automationRepository;
        this.instagramAccountService = instagramAccountService;
        this.instagramApiClient = instagramApiClient;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    public void lockPendingNextPostAutomations() {
        log.info("NextPostLocker: heartbeat — job triggered.");

        List<OwnedAutomation> pending;
        try {
            pending = automationRepository.findAllPendingNextPost();
        } catch (Exception e) {
            log.warn("NextPostLocker: failed to load pending automations: {}", e.getMessage());
            return;
        }

        if (pending == null || pending.isEmpty()) {
            log.info("NextPostLocker: no pending NEXT_POST automations.");
            return;
        }

        log.info("NextPostLocker: found {} pending automation(s).", pending.size());

        Map<String, List<Automation>> byOwner = pending.stream()
                .collect(Collectors.groupingBy(
                        OwnedAutomation::uid,
                        Collectors.mapping(OwnedAutomation::automation, Collectors.toList())
                ));

        byOwner.forEach((uid, list) -> {
            if (uid == null || uid.isBlank()) return;
            try {
                processOwner(uid, list);
            } catch (Exception e) {
                log.warn("NextPostLocker: error processing uid={}: {}", uid, e.getMessage());
            }
        });
    }

    private void processOwner(String uid, List<Automation> automations) {
        Optional<InstagramAccount> accountOpt = instagramAccountService.find(uid);
        if (accountOpt.isEmpty() || accountOpt.get().getAccessToken() == null) {
            log.info("NextPostLocker: uid={} has no IG account, skipping.", uid);
            return;
        }

        MetaMediaResponse media;
        try {
            media = instagramApiClient.fetchMedia(accountOpt.get().getAccessToken());
        } catch (Exception e) {
            log.warn("NextPostLocker: fetchMedia failed for uid={}: {}", uid, e.getMessage());
            return;
        }

        if (media == null || media.data() == null || media.data().isEmpty()) {
            log.info("NextPostLocker: uid={} — Instagram returned NO media items.", uid);
            return;
        }

        log.info("NextPostLocker: uid={} — Instagram returned {} media item(s).",
                uid, media.data().size());

        for (Automation a : automations) {
            Set<String> baseline = a.getBaselineMediaIds() == null
                    ? Set.of()
                    : new HashSet<>(a.getBaselineMediaIds());

            Instant createdAt = a.getCreatedAt() != null
                    ? a.getCreatedAt().toInstant()
                    : Instant.EPOCH;

            log.info("NextPostLocker: checking automation={} baselineSize={} createdAt={}",
                    a.getId(), baseline.size(), createdAt);

            media.data().forEach(m -> {
                boolean inBaseline = m.id() != null && baseline.contains(m.id());
                Instant ts = parseTimestamp(m.timestamp());
                boolean afterCreated = ts != null && ts.isAfter(createdAt);
                log.info("NextPostLocker:   candidate id={} timestamp={} inBaseline={} afterCreated={}",
                        m.id(), m.timestamp(), inBaseline, afterCreated);
            });

            Optional<MetaMediaResponse.MediaItem> next = media.data().stream()
                    .filter(m -> m.id() != null && !baseline.contains(m.id()))
                    .filter(m -> {
                        Instant ts = parseTimestamp(m.timestamp());
                        return ts != null && ts.isAfter(createdAt);
                    })
                    .min(Comparator.comparing(m -> parseTimestamp(m.timestamp())));

            if (next.isEmpty()) {
                log.info("NextPostLocker: uid={} automation={} — still no new post.",
                        uid, a.getId());
                continue;
            }

            String newPostId = next.get().id();
            log.info("NextPostLocker: found new post id={} for automation={}",
                    newPostId, a.getId());

            a.setTargetPostId(newPostId);
            a.setNextPostLockedAt(new Date());
            try {
                automationRepository.save(uid, a);
                log.info("NextPostLocker: locked uid={} automation={} -> postId={}",
                        uid, a.getId(), newPostId);
            } catch (Exception e) {
                log.warn("NextPostLocker: save failed uid={} automation={}: {}",
                        uid, a.getId(), e.getMessage());
            }
        }
    }

    private Instant parseTimestamp(String ts) {
        if (ts == null || ts.isBlank()) return null;
        try {
            String normalized = ts.replaceAll("([+-])(\\d{2})(\\d{2})$", "$1$2:$3");
            return OffsetDateTime.parse(normalized).toInstant();
        } catch (Exception e) {
            log.debug("NextPostLocker: parseTimestamp failed for '{}': {}", ts, e.getMessage());
            return null;
        }
    }
}