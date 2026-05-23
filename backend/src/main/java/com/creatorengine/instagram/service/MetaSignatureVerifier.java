package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies the {@code X-Hub-Signature-256} header that Meta attaches
 * to every webhook POST.
 *
 * <p>The signature is {@code "sha256=" + HMAC-SHA256(appSecret, rawBody)}.
 * We MUST compare against the raw request bytes — re-serialising
 * Spring's parsed JSON would change whitespace and break the HMAC.
 * That's why the controller takes {@code String rawBody}, not
 * {@code Map<String, Object>}.</p>
 *
 * <p>Without this check, anyone could POST anything to our public
 * webhook endpoint and we'd treat it as authentic.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String SIG_PREFIX = "sha256=";

    private final AppProperties props;

    /**
     * @param signatureHeader the value of the {@code X-Hub-Signature-256} header
     * @param rawBody         the exact bytes Meta POSTed
     * @return true iff the signature is valid for this body
     */
    public boolean isValid(String signatureHeader, String rawBody) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIG_PREFIX)) {
            log.debug("Webhook signature missing or malformed: {}", signatureHeader);
            return false;
        }
        String appSecret = props.getMeta().getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("Cannot verify webhook signature: META_APP_SECRET is not configured.");
            return false;
        }

        String providedHex = signatureHeader.substring(SIG_PREFIX.length()).toLowerCase();
        String computedHex = hmacSha256Hex(appSecret, rawBody == null ? "" : rawBody);
        // Constant-time compare to defeat timing oracles.
        return constantTimeEquals(providedHex, computedHex);
    }

    private static String hmacSha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] sig = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(sig.length * 2);
            for (byte b : sig) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
