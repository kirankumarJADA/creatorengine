package com.creatorengine.ai.controller;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.creatorengine.ai.dto.GenerateMessageResponse;
import com.creatorengine.ai.service.AiMessageService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI message-assistant endpoint.
 *
 * <p>Single endpoint: {@code POST /api/ai/generate-message} returns
 * 3 DM template suggestions tailored to the user's goal/tone/audience/CTA.
 * Authentication uses the standard JWT path; uid is captured just for
 * logging (no per-user data is persisted by this feature).</p>
 *
 * <p>The endpoint never errors on AI failures — see
 * {@link AiMessageService} for the provider fallback chain.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "AI-powered message suggestions for automations")
public class AiMessageController {

    private final AiMessageService service;

    @PostMapping("/generate-message")
    @Operation(summary = "Generate 3 DM template suggestions tailored to the user's brief")
    public ResponseEntity<ApiResponse<GenerateMessageResponse>> generate(
            @Valid @RequestBody GenerateMessageRequest req
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        log.info("AI generate-message requested uid={} tone={} goalLen={}",
                uid, req.tone(), req.goal() == null ? 0 : req.goal().length());

        GenerateMessageResponse resp = service.generate(req);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
