package com.creatorengine.ai.controller;

import com.creatorengine.ai.dto.GenerateMessageRequest;
import com.creatorengine.ai.dto.GenerateMessageResponse;
import com.creatorengine.ai.service.AiMessageService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Assistant", description = "AI-powered message suggestions for automations")
public class AiMessageController {

    private static final Logger log = LoggerFactory.getLogger(AiMessageController.class);

    private final AiMessageService service;

    public AiMessageController(AiMessageService service) {
        this.service = service;
    }

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