package com.creatorengine.security;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int  MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 30 * 60L;

    private record AttemptRecord(int count, Instant lockedUntil) {}

    private final Map<String, AttemptRecord> cache = new ConcurrentHashMap<>();

    public void recordFailure(String email) {
        String key = normalize(email);
        AttemptRecord existing = cache.getOrDefault(key, new AttemptRecord(0, null));
        int newCount = existing.count() + 1;
        Instant lockedUntil = newCount >= MAX_ATTEMPTS
                ? Instant.now().plusSeconds(LOCK_SECONDS)
                : existing.lockedUntil();
        cache.put(key, new AttemptRecord(newCount, lockedUntil));
    }

    public void recordSuccess(String email) {
        cache.remove(normalize(email));
    }

    public boolean isLocked(String email) {
        AttemptRecord record = cache.get(normalize(email));
        if (record == null || record.lockedUntil() == null) return false;
        if (Instant.now().isAfter(record.lockedUntil())) {
            cache.remove(normalize(email));
            return false;
        }
        return true;
    }

    public long minutesRemaining(String email) {
        AttemptRecord record = cache.get(normalize(email));
        if (record == null || record.lockedUntil() == null) return 0;
        long seconds = record.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(1, (long) Math.ceil(seconds / 60.0));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}