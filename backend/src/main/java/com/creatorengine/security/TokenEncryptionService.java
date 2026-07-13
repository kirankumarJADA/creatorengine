package com.creatorengine.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM authenticated encryption for sensitive tokens stored at rest
 * (Instagram access tokens in Firestore).
 *
 * <p>Output format: {@code "enc:v1:" + base64(12-byte IV || ciphertext || 16-byte auth-tag)}.
 * The {@code enc:v1:} prefix lets us distinguish freshly-encrypted values from any
 * legacy plaintext tokens already stored — so we can migrate transparently as
 * the token-refresh scheduler re-saves each account.</p>
 */
@Service
public class TokenEncryptionService {

    private static final String PREFIX           = "enc:v1:";
    private static final String TRANSFORMATION   = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH_BYTES  = 12;
    private static final int    TAG_LENGTH_BITS  = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public TokenEncryptionService(@Value("${TOKEN_ENCRYPTION_KEY:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "TOKEN_ENCRYPTION_KEY is not configured. " +
                "Generate one with: openssl rand -base64 32  (or the PowerShell snippet in your runbook).");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY is not valid base64.", e);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException(
                "TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes (256 bits). Got " + decoded.length + ".");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    /** Returns null/empty input unchanged. Already-encrypted input is returned as-is. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        if (plaintext.startsWith(PREFIX))             return plaintext;

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ct, 0, combined, iv.length, ct.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Token encryption failed", e);
        }
    }

    /**
     * Decrypts a stored token. If the value does NOT carry the {@code enc:v1:}
     * prefix it is returned unchanged — this is the legacy-plaintext fallback
     * that lets pre-encryption tokens keep working until they're re-saved.
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        if (!stored.startsWith(PREFIX))         return stored;  // legacy plaintext

        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (combined.length < IV_LENGTH_BYTES + 16) {
                throw new IllegalArgumentException("Ciphertext too short.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            byte[] ct = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, IV_LENGTH_BYTES, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Token decryption failed (key mismatch or corrupted data).", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}