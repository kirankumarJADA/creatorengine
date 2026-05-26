package com.creatorengine.auth.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Thin wrapper around the Firebase Auth REST API for the one thing the
 * Admin SDK can't do server-side: verify a user's password.
 *
 * <p>The Admin SDK can create users and reset passwords, but the only
 * way to verify a plaintext password is to POST it to:</p>
 *
 * <pre>https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={apiKey}</pre>
 *
 * <p>We deliberately keep this isolated in one class so the REST
 * dependency doesn't bleed into AuthService.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthClient {

    private static final String SEND_OOB_CODE_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode";

    private final AppProperties props;

    // Lazy so tests can swap a fake; built once on first call.
    private RestClient restClient;

    // Constructor-injected indirectly via Spring's default ObjectMapper bean
    @Autowired
    private ObjectMapper objectMapper;

    private synchronized RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(SIGN_IN_URL)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return restClient;
    }

    /**
     * Verifies a (email, password) pair against Firebase Authentication.
     *
     * @return the Firebase UID on success
     * @throws UnauthorizedException if Firebase rejects the credentials
     * @throws BadRequestException   if the Firebase Web API key is misconfigured
     */
    public String verifyPassword(String email, String password) {
        String apiKey = props.getFirebase().getWebApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException(
                    "Firebase Web API key is not configured (FIREBASE_WEB_API_KEY).");
        }

        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", false
        );

        try {
            SignInResponse resp = client()
                    .post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .body(body)
                    .retrieve()
                    .body(SignInResponse.class);

            if (resp == null || resp.localId() == null) {
                throw new UnauthorizedException("Invalid email or password.");
            }
            return resp.localId();

      } catch (HttpClientErrorException ex) {
            String firebaseError = parseErrorMessage(ex.getResponseBodyAsString());
            log.info("Firebase signIn rejected for email='{}': code='{}', status={}",
                    email, firebaseError, ex.getStatusCode().value());
            throw new UnauthorizedException(translateFirebaseError(firebaseError));
        } catch (org.springframework.web.client.RestClientException ex) {
            log.error("Firebase signIn network error: {}", ex.getMessage());
            throw new UnauthorizedException(
                    "Authentication service is temporarily unavailable. Please try again.");
        }
    }

    private String translateFirebaseError(String firebaseCode) {
        return switch (firebaseCode) {
            case "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS"
                    -> "Invalid email or password.";
            case "USER_DISABLED"
                    -> "This account has been disabled.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER"
                    -> "Too many failed attempts. Please try again later.";
            case "MISSING_PASSWORD"
                    -> "Password is required.";
            case "INVALID_EMAIL"
                    -> "That email isn't formatted correctly.";
            case "OPERATION_NOT_ALLOWED"
                    -> "Email/password sign-in is not enabled in this Firebase project.";
            default -> "Sign-in failed (code: " + firebaseCode + ").";
        };
    }

    private String translateFirebaseError(String firebaseCode) {
        // Map Firebase error codes to user-facing messages. We deliberately
        // collapse "user not found" and "wrong password" into the same
        // generic message — it's a standard security practice to prevent
        // email enumeration.
        return switch (firebaseCode) {
            case "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS"
                    -> "Invalid email or password.";
            case "USER_DISABLED"
                    -> "This account has been disabled.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER"
                    -> "Too many failed attempts. Please try again later.";
            default -> "Invalid email or password.";
        };
    }

    // ─── DTOs for the Firebase REST response ─────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SignInResponse(String localId, String email) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorEnvelope(ErrorBody error) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ErrorBody(int code, String message) {}
    }
    public void sendPasswordResetEmail(String email, String continueUrl) {
        String apiKey = props.getFirebase().getWebApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException(
                    "Firebase Web API key is not configured (FIREBASE_WEB_API_KEY).");
        }

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("requestType", "PASSWORD_RESET");
        body.put("email", email);
        if (continueUrl != null && !continueUrl.isBlank()) {
            body.put("continueUrl", continueUrl);
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(SEND_OOB_CODE_URL)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            client.post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            String firebaseError = parseErrorMessage(ex.getResponseBodyAsString());
            log.warn("Firebase sendOobCode rejected for '{}': code='{}', status={}",
                    email, firebaseError, ex.getStatusCode().value());
            throw new UnauthorizedException("Could not send password reset email.");
        }
    }
}
