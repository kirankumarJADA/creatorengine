package com.creatorengine.auth.service;

import com.creatorengine.config.AppProperties;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.UnauthorizedException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class FirebaseAuthClient {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthClient.class);

    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword";

    private static final String SEND_OOB_CODE_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode";

    private final AppProperties props;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    public FirebaseAuthClient(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    private synchronized RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(SIGN_IN_URL)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }

        return restClient;
    }

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

    private String parseErrorMessage(String body) {
        try {
            ErrorEnvelope envelope = objectMapper.readValue(body, ErrorEnvelope.class);
            if (envelope != null && envelope.error() != null && envelope.error().message() != null) {
                return envelope.error().message();
            }
        } catch (Exception ignored) {
            // Fall through to generic message.
        }

        return "UNKNOWN";
    }

    private String translateFirebaseError(String firebaseCode) {
        return switch (firebaseCode) {
            case "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" ->
                    "Invalid email or password.";
            case "USER_DISABLED" ->
                    "This account has been disabled.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                    "Too many failed attempts. Please try again later.";
            case "MISSING_PASSWORD" ->
                    "Password is required.";
            case "INVALID_EMAIL" ->
                    "That email isn't formatted correctly.";
            case "OPERATION_NOT_ALLOWED" ->
                    "Email/password sign-in is not enabled in this Firebase project.";
            default ->
                    "Invalid email or password.";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SignInResponse(String localId, String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorEnvelope(ErrorBody error) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ErrorBody(int code, String message) {
        }
    }
}