package com.creatorengine.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-fast environment validation for the {@code prod} profile.
 *
 * <p>Boots only when {@code SPRING_PROFILES_ACTIVE=prod}. Checks that
 * every production-critical setting has a real value (not the
 * {@code application.yml} placeholder defaults that are perfectly fine
 * for local dev but would be a security incident if shipped). If
 * anything fails, the application context refuses to start and the
 * deployment platform (Render, Fly, etc.) shows the exact list of
 * problems in its logs — much friendlier than discovering at first
 * request that the JWT secret is "CHANGE_ME_…".</p>
 *
 * <p>This validator deliberately does <b>not</b> run in dev: developers
 * sometimes want to spin up the backend without Meta credentials to
 * iterate on UI or Firestore data. The harshness is purely a prod
 * concern.</p>
 *
 * <h3>What gets checked</h3>
 * <ul>
 *   <li>JWT secret is set, ≥ 32 bytes, and isn't the placeholder.</li>
 *   <li>Firebase project id and web API key are set.</li>
 *   <li>Exactly one Firebase credential source is provided.</li>
 *   <li>Meta App ID + secret + verify-token are set (not placeholder).</li>
 *   <li>Meta redirect URIs use HTTPS and are not localhost.</li>
 *   <li>CORS allowed-origins is set and contains no localhost entries.</li>
 *   <li>{@code FRONTEND_BASE_URL} is HTTPS and not localhost.</li>
 * </ul>
 */
@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class EnvironmentValidator {

    /** Sentinel strings from {@code application.yml} that must not survive into prod. */
    private static final String JWT_PLACEHOLDER = "CHANGE_ME";
    private static final String META_VERIFY_PLACEHOLDER = "change-me-meta-verify-token";

    /** Minimum HMAC-SHA256 key length in bytes (≥256 bits). */
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private final AppProperties props;

    @PostConstruct
    public void validate() {
        log.info("Running production environment validation (profile=prod)…");
        List<String> problems = new ArrayList<>();

        validateJwt(problems);
        validateFirebase(problems);
        validateMeta(problems);
        validateCors(problems);
        validateFrontendUrl(problems);

        if (!problems.isEmpty()) {
            String banner = """

                ════════════════════════════════════════════════════════════════════
                  Production environment validation FAILED.
                  The application will not start until these are resolved.
                ════════════════════════════════════════════════════════════════════
                """;
            String list = problems.stream()
                    .map(p -> "  • " + p)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            log.error("{}{}\n", banner, list);
            throw new IllegalStateException(
                    "Production environment validation failed with "
                    + problems.size() + " problem(s). See logs above.");
        }
        log.info("✅ Production environment validation passed.");
    }

    // ─── JWT ─────────────────────────────────────────────────────
    private void validateJwt(List<String> problems) {
        String secret = props.getSecurity().getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            problems.add("JWT_SECRET is not set. Generate one with: openssl rand -base64 64");
            return;
        }
        if (secret.contains(JWT_PLACEHOLDER)) {
            problems.add("JWT_SECRET is still the placeholder value. Replace it with a real secret.");
        }
        // Base64 isn't required, but a short raw string is. Length check after decode-ish:
        // we approximate by counting bytes — anything < 32 bytes is too small for HMAC-SHA256.
        if (secret.getBytes().length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET is too short — needs at least "
                    + MIN_JWT_SECRET_BYTES + " bytes (256 bits) for HMAC-SHA256.");
        }
    }

    // ─── Firebase ────────────────────────────────────────────────
    private void validateFirebase(List<String> problems) {
        var fb = props.getFirebase();
        if (!StringUtils.hasText(fb.getProjectId())) {
            problems.add("FIREBASE_PROJECT_ID is not set.");
        }
        if (!StringUtils.hasText(fb.getWebApiKey())) {
            problems.add("FIREBASE_WEB_API_KEY is not set (required for password authentication).");
        }
        boolean hasPath = StringUtils.hasText(fb.getCredentialsPath());
        boolean hasJson = StringUtils.hasText(fb.getCredentialsJson());
        if (!hasPath && !hasJson) {
            problems.add("Neither FIREBASE_CREDENTIALS_PATH nor FIREBASE_CREDENTIALS_JSON is set. "
                    + "Provide service-account credentials.");
        }
        if (hasPath && hasJson) {
            problems.add("Both FIREBASE_CREDENTIALS_PATH and FIREBASE_CREDENTIALS_JSON are set. "
                    + "Provide exactly one.");
        }
    }

    // ─── Meta / Instagram ────────────────────────────────────────
    private void validateMeta(List<String> problems) {
        var meta = props.getMeta();
        if (!StringUtils.hasText(meta.getAppId())) {
            problems.add("META_APP_ID is not set.");
        }
        if (!StringUtils.hasText(meta.getAppSecret())) {
            problems.add("META_APP_SECRET is not set (required for webhook signature verification).");
        }
        String verify = meta.getVerifyToken();
        if (!StringUtils.hasText(verify) || META_VERIFY_PLACEHOLDER.equals(verify)) {
            problems.add("META_VERIFY_TOKEN is not set or still has the placeholder value.");
        } else if (verify.length() < 16) {
            problems.add("META_VERIFY_TOKEN is too short — use a high-entropy string (≥16 chars).");
        }

        // Meta requires HTTPS for OAuth redirect URIs registered against an app
        // in production mode. Localhost is fine for dev only.
        requireHttpsNonLocalhost(problems, "META_REDIRECT_URI", meta.getRedirectUri());
        requireHttpsNonLocalhost(problems, "META_SUCCESS_REDIRECT_URI", meta.getSuccessRedirectUri());
    }

    // ─── CORS ────────────────────────────────────────────────────
    private void validateCors(List<String> problems) {
        String origins = props.getCors().getAllowedOrigins();
        if (!StringUtils.hasText(origins)) {
            problems.add("CORS_ALLOWED_ORIGINS is not set. The browser will block all API calls.");
            return;
        }
        for (String origin : origins.split(",")) {
            String trimmed = origin.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("localhost") || trimmed.contains("127.0.0.1")) {
                problems.add("CORS_ALLOWED_ORIGINS contains localhost ('" + trimmed
                        + "') — production should only allow your real frontend origin(s).");
            }
            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
                problems.add("CORS_ALLOWED_ORIGINS entry '" + trimmed
                        + "' must include a scheme (https://…).");
            }
        }
    }

    private void validateFrontendUrl(List<String> problems) {
        String url = props.getFrontendBaseUrl();
        if (!StringUtils.hasText(url)) {
            problems.add("FRONTEND_BASE_URL is not set.");
            return;
        }
        if (url.contains("localhost") || url.contains("127.0.0.1")) {
            problems.add("FRONTEND_BASE_URL still points at localhost.");
        }
        if (!url.startsWith("https://")) {
            problems.add("FRONTEND_BASE_URL must use HTTPS in production (got: " + url + ").");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────
    private void requireHttpsNonLocalhost(List<String> problems, String label, String value) {
        if (!StringUtils.hasText(value)) {
            problems.add(label + " is not set.");
            return;
        }
        if (value.contains("localhost") || value.contains("127.0.0.1")) {
            problems.add(label + " contains localhost — Meta requires a real HTTPS URL.");
        }
        if (!value.startsWith("https://")) {
            problems.add(label + " must use HTTPS in production (got: " + value + ").");
        }
    }
}
