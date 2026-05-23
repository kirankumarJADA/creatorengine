package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.dto.SimulateWebhookRequest;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.instagram.service.WebhookService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Dev-only testing endpoint for the webhook pipeline.
 *
 * <p>Gated to the {@code dev} Spring profile — registering this bean
 * in production would let any authenticated user fabricate webhook
 * events for themselves, which is a polite way of saying "auditor
 * nightmare". Don't enable in prod.</p>
 *
 * <p>Two endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/webhook/test/simulate} — bypass signature
 *       verification and run an event through the same dispatch
 *       pipeline a real webhook would hit. The {@code receivingAccountId}
 *       defaults to the caller's connected IG account so attribution works.</li>
 *   <li>{@code POST /api/webhook/test/raw} — feed a raw Meta-shaped
 *       payload directly to the parser + dispatcher. Useful for
 *       replaying real payloads captured from the Meta dashboard.</li>
 * </ul>
 */
@Slf4j
@Profile("dev")
@RestController
@RequestMapping("/api/webhook/test")
@RequiredArgsConstructor
@Tag(name = "Webhook (dev)", description = "Dev-only webhook simulator")
public class WebhookTestController {

    private final WebhookService webhookService;
    private final InstagramAccountService accountService;

    @PostMapping("/simulate")
    @Operation(summary = "Synthesize a single webhook event and run it through the pipeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulate(
            @Valid @RequestBody SimulateWebhookRequest req) {

        // If the caller didn't specify a receiving account, use theirs.
        String receivingAccount = req.receivingAccountId();
        if (receivingAccount == null || receivingAccount.isBlank()) {
            String uid = SecurityUtils.getCurrentUserId();
            receivingAccount = accountService.find(uid)
                    .map(a -> a.getInstagramUserId())
                    .orElse(null);
        }

        WebhookEventDto event = WebhookEventDto.builder()
                .type(req.type())
                .message(req.message())
                .username(req.username())
                .instagramUserId(req.instagramUserId())
                .postId(req.postId())
                .commentId(req.commentId())
                .messageId(req.messageId())
                .eventTime(Instant.now())
                .receivingAccountId(receivingAccount)
                .build();

        boolean attributed = webhookService.dispatch(event, null);
        log.info("[dev-simulator] Dispatched fake event type={} attributed={}",
                event.type(), attributed);

        return ResponseEntity.ok(ApiResponse.ok(
                "Event simulated.",
                Map.of(
                        "attributed", attributed,
                        "receivingAccountId", receivingAccount == null ? "" : receivingAccount
                )));
    }

    @PostMapping("/raw")
    @Operation(summary = "Replay a raw Meta payload through the parser + dispatcher (no signature check)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> raw(@RequestBody String rawBody) {
        var result = webhookService.processUnsigned(rawBody);
        return ResponseEntity.ok(ApiResponse.ok(
                "Payload replayed.",
                Map.of(
                        "accepted",   result.accepted(),
                        "attributed", result.attributed(),
                        "orphaned",   result.orphaned()
                )));
    }
}
