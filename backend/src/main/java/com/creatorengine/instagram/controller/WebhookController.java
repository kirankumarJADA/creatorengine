package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.service.WebhookService;
import com.creatorengine.instagram.service.WebhookService.ProcessingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Meta-facing webhook receiver.
 *
 * <p>Both endpoints live under {@code /api/webhook} and are exempted
 * from JWT auth in {@code SecurityConfig.PUBLIC_PATHS} — Meta calls
 * them directly and would never have a CreatorEngine JWT.</p>
 *
 * <p>Authentication is instead:</p>
 * <ul>
 *   <li>GET: the {@code hub.verify_token} query param must match our
 *       configured {@code META_VERIFY_TOKEN}.</li>
 *   <li>POST: the {@code X-Hub-Signature-256} header must be a valid
 *       HMAC-SHA256 of the raw body using our {@code META_APP_SECRET}.
 *       Verification lives in {@link com.creatorengine.instagram.service.MetaSignatureVerifier}.</li>
 * </ul>
 *
 * <p>Meta expects a 200 OK fast. We do the bare minimum on the request
 * thread — parse, persist, return — and offload anything heavyweight
 * (sending DMs, running automations) to a downstream pipeline.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "Meta Instagram webhook receiver")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Meta verification handshake (subscribe/echo hub.challenge)")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode",         required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge",    required = false) String challenge) {

        if (webhookService.isValidVerification(mode, token)) {
            // Meta requires we echo the challenge verbatim, as text/plain.
            return ResponseEntity.ok(challenge != null ? challenge : "");
        }
        log.warn("Webhook verification rejected — mode='{}', tokenMatched=false", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }

    @PostMapping(consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive a batch of webhook events from Meta")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            // String, NOT a parsed object — we MUST hash the raw bytes for signature
            // verification. Re-serialising Spring's parsed JSON would change whitespace.
            @RequestBody(required = false) String rawBody) {

        ProcessingResult result = webhookService.processIncoming(signature, rawBody);

        if (!result.accepted()) {
            // Returning non-2xx tells Meta to retry. For genuine signature
            // failures we WANT them to retry rarely (we'll keep rejecting),
            // so 401 is honest. Don't 200 OK on bad signatures.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(result.error() != null
                            ? result.error()
                            : "Webhook rejected."));
        }

        return ResponseEntity.ok(ApiResponse.ok(
                "Webhook accepted.",
                Map.of(
                        "attributed", result.attributed(),
                        "orphaned",   result.orphaned()
                )));
    }
}
