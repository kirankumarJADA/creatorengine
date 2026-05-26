package com.creatorengine.instagram.service;

import com.creatorengine.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetaSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String SIG_PREFIX = "sha256=";
    private static final HexFormat HEX = HexFormat.of();

    private final AppProperties props;

    public boolean isValid(String signatureHeader, byte[] rawBody) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIG_PREFIX)) {
            return false;
        }

        String appSecret = props.getMeta().getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("META_APP_SECRET not configured.");
            return false;
        }
        appSecret = appSecret.strip();

        byte[] provided;
        try {
            provided = HEX.parseHex(
                signatureHeader.substring(SIG_PREFIX.length()).toLowerCase()
            );
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] computed = hmacSha256(
            appSecret,
            rawBody == null ? new byte[0] : rawBody
        );

        boolean match = MessageDigest.isEqual(provided, computed);

        // TEMPORARY DEBUG — remove once signature verification is confirmed working.
        if (!match) {
            log.warn("META SIG MISMATCH providedHex={} computedHex={} bodyLen={} secretLen={} secretFirst4={} secretLast4={}",
                HEX.formatHex(provided),
                HEX.formatHex(computed),
                rawBody == null ? 0 : rawBody.length,
                appSecret.length(),
                appSecret.length() >= 4 ? appSecret.substring(0, 4) : "?",
                appSecret.length() >= 4 ? appSecret.substring(appSecret.length() - 4) : "?");
        }

        return match;
    }

    private static byte[] hmacSha256(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGO
            ));
            return mac.doFinal(body);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }
}