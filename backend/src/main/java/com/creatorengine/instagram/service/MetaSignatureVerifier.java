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

/**
 * Verifies the {@code X-Hub-Signature-256} header that Meta attaches to
 * every webhook POST.
 *
 * <p>The signature is {@code "sha256=" + HMAC-SHA256(appSecret, rawBody)}.
 * We hash the exact raw request bytes — converting through {@code String}
 * would mutate non-ASCII content (emoji in IG comments, accented usernames)
 * and break the HMAC, which is why the controller binds {@code byte[]} not
 * {@code String}.</p>
 *
 * <p>For Instagram webhooks set up under "API setup with Instagram login",
 * {@code appSecret} must be the <b>Instagram App Secret</b> (from the IG
 * product config), not the Facebook App's basic App Secret.</p>
 */
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

        return MessageDigest.isEqual(provided, computed);
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