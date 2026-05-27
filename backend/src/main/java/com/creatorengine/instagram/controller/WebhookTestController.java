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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/webhook/test")
@Tag(name = "Webhook (dev)", description = "Dev-only webhook simulator")
public class WebhookTestController {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestController.class);

    private final WebhookService webhookService;
    private final InstagramAccountService accountService;

    public WebhookTestController(
            WebhookService webhookService,
            InstagramAccountService accountService
    ) {
        this.webhookService = webhookService;
        this.accountService = accountService;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Synthesize a single webhook event and run it through the pipeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulate(
            @Valid @RequestBody SimulateWebhookRequest req
    ) {
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
    @Operation(summary = "Replay a raw Meta payload through the parser + dispatcher")
    public ResponseEntity<ApiResponse<Map<String, Object>>> raw(@RequestBody String rawBody) {
        var result = webhookService.processUnsigned(rawBody);

        return ResponseEntity.ok(ApiResponse.ok(
                "Payload replayed.",
                Map.of(
                        "accepted", result.accepted(),
                        "attributed", result.attributed(),
                        "orphaned", result.orphaned()
                )));
    }
}