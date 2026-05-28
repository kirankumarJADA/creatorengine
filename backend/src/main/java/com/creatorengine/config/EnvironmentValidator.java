package com.creatorengine.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Profile("prod")
@Component
public class EnvironmentValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    private static final String JWT_PLACEHOLDER = "CHANGE_ME";
    private static final String META_VERIFY_PLACEHOLDER = "change-me-meta-verify-token";
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private final AppProperties props;

    public EnvironmentValidator(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void validate() {
        log.info("Running production environment validation (profile=prod).");

        List<String> problems = new ArrayList<>();

        validateJwt(problems);
        validateFirebase(problems);
        validateMeta(problems);
        validateCors(problems);
        validateFrontendUrl(problems);

        if (!problems.isEmpty()) {
            String list = problems.stream()
                    .map(p -> "  - " + p)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            log.error("Production environment validation FAILED:\n{}", list);

            throw new IllegalStateException(
                    "Production environment validation failed with "
                            + problems.size() + " problem(s). See logs above.");
        }

        log.info("Production environment validation passed.");
    }

    private void validateJwt(List<String> problems) {
        String secret = props.getSecurity().getJwt().getSecret();

        if (!StringUtils.hasText(secret)) {
            problems.add("JWT_SECRET is not set. Generate one with: openssl rand -base64 64");
            return;
        }

        if (secret.contains(JWT_PLACEHOLDER)) {
            problems.add("JWT_SECRET is still the placeholder value. Replace it with a real secret.");
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET is too short - needs at least "
                    + MIN_JWT_SECRET_BYTES + " bytes (256 bits) for HMAC-SHA256.");
        }
    }

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
            problems.add("Neither FIREBASE_CREDENTIALS_PATH nor FIREBASE_CREDENTIALS_JSON is set.");
        }

        if (hasPath && hasJson) {
            problems.add("Both FIREBASE_CREDENTIALS_PATH and FIREBASE_CREDENTIALS_JSON are set. Provide exactly one.");
        }
    }

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
            problems.add("META_VERIFY_TOKEN is too short - use a high-entropy string with at least 16 chars.");
        }

        requireHttpsNonLocalhost(problems, "META_REDIRECT_URI", meta.getRedirectUri());
        requireHttpsNonLocalhost(problems, "META_SUCCESS_REDIRECT_URI", meta.getSuccessRedirectUri());
    }

    private void validateCors(List<String> problems) {
        String origins = props.getCors().getAllowedOrigins();

        if (!StringUtils.hasText(origins)) {
            problems.add("CORS_ALLOWED_ORIGINS is not set. The browser will block all API calls.");
            return;
        }

        for (String origin : origins.split(",")) {
            String trimmed = origin.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.contains("localhost") || trimmed.contains("127.0.0.1")) {
                problems.add("CORS_ALLOWED_ORIGINS contains localhost ('" + trimmed + "').");
            }

            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
                problems.add("CORS_ALLOWED_ORIGINS entry '" + trimmed + "' must include a scheme.");
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

    private void requireHttpsNonLocalhost(List<String> problems, String label, String value) {
        if (!StringUtils.hasText(value)) {
            problems.add(label + " is not set.");
            return;
        }

        if (value.contains("localhost") || value.contains("127.0.0.1")) {
            problems.add(label + " contains localhost - Meta requires a real HTTPS URL.");
        }

        if (!value.startsWith("https://")) {
            problems.add(label + " must use HTTPS in production (got: " + value + ").");
        }
    }
}