package com.creatorengine.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final long OTP_TTL_SECONDS = 600;
    private static final int MAX_ATTEMPTS = 5;

    private record OtpEntry(String otp, Instant expiresAt, int attempts) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generate(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(
                otp,
                Instant.now().plusSeconds(OTP_TTL_SECONDS),
                0
        ));
        log.debug("OTP generated for email={}", email);
        return otp;
    }

    public boolean verify(String email, String otp) {
        String key = email.toLowerCase();
        OtpEntry entry = store.get(key);

        if (entry == null) {
            log.warn("OTP verify: no entry for email={}", email);
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            log.warn("OTP verify: expired for email={}", email);
            return false;
        }

        if (entry.attempts() >= MAX_ATTEMPTS) {
            store.remove(key);
            log.warn("OTP verify: max attempts exceeded for email={}", email);
            return false;
        }

        if (!entry.otp().equals(otp)) {
            store.put(key, new OtpEntry(
                    entry.otp(), entry.expiresAt(), entry.attempts() + 1));
            log.warn("OTP verify: wrong code for email={} attempt={}",
                    email, entry.attempts() + 1);
            return false;
        }

        store.remove(key);
        log.info("OTP verified for email={}", email);
        return true;
    }

    public boolean hasPending(String email) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return true;
    }
}