package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.service.WebhookService;
import com.creatorengine.instagram.service.WebhookService.ProcessingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@Tag(name = "Webhook", description = "Meta Instagram webhook receiver")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Meta verification handshake")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (webhookService.isValidVerification(mode, token)) {
            return ResponseEntity.ok(challenge != null ? challenge : "");
        }

        log.warn("Webhook verification rejected - mode='{}', tokenMatched=false", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }

    @PostMapping(consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive a batch of webhook events from Meta")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) byte[] rawBody
    ) {
        ProcessingResult result = webhookService.processIncoming(signature, rawBody);

        if (!result.accepted()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(result.error() != null
                            ? result.error()
                            : "Webhook rejected."));
        }

        return ResponseEntity.ok(ApiResponse.ok(
                "Webhook accepted.",
                Map.of(
                        "attributed", result.attributed(),
                        "orphaned", result.orphaned()
                )));
    }

    // ===== TEMPORARY TEST ENDPOINT — REMOVE BEFORE PRODUCTION =====
    @PostMapping(value = "/replay", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "TEMP: replay an unsigned payload to test the engine end-to-end")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replay(@RequestBody String rawBody) {
        log.warn("TEMP /api/webhook/replay invoked — unsigned test endpoint, REMOVE before production.");
        ProcessingResult result = webhookService.processUnsigned(rawBody);
        return ResponseEntity.ok(ApiResponse.ok(
                "Replayed.",
                Map.of(
                        "attributed", result.attributed(),
                        "orphaned", result.orphaned()
                )));
    }
}