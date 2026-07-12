package com.creatorengine.automation.controller;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.dto.ToggleRequest;
import com.creatorengine.automation.service.AutomationService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MULTI-ACCOUNT: Every request must include the active Instagram account ID
 * as a request header: X-IG-Account-Id: {instagramUserId}
 *
 * This scopes all automations to the selected account. The frontend sends
 * this header automatically via the axios interceptor in api.js.
 */
@RestController
@RequestMapping("/api/automations")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    private String requireIgAccountId(String igAccountId) {
        if (igAccountId == null || igAccountId.isBlank()) {
            throw new BadRequestException("X-IG-Account-Id header is required.");
        }
        return igAccountId.trim();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AutomationResponse>>> list(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        if (igAccountId == null || igAccountId.isBlank()) {
            // No account selected — return empty list
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<AutomationResponse> automations = automationService.listForAccount(uid, igAccountId.trim());
        return ResponseEntity.ok(ApiResponse.ok(automations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> get(
            @PathVariable String id,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse automation = automationService.getForAccount(uid, requireIgAccountId(igAccountId), id);
        return ResponseEntity.ok(ApiResponse.ok(automation));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AutomationResponse>> create(
            @Valid @RequestBody AutomationRequest req,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse created = automationService.createForAccount(uid, requireIgAccountId(igAccountId), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Automation created.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody AutomationRequest req,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse updated = automationService.updateForAccount(uid, requireIgAccountId(igAccountId), id, req);
        return ResponseEntity.ok(ApiResponse.ok("Automation updated.", updated));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AutomationResponse>> toggle(
            @PathVariable String id,
            @Valid @RequestBody ToggleRequest req,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse toggled = automationService.toggleForAccount(uid, requireIgAccountId(igAccountId), id, req.enabled());
        return ResponseEntity.ok(ApiResponse.ok(
                req.enabled() ? "Automation enabled." : "Automation disabled.", toggled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        automationService.deleteForAccount(uid, requireIgAccountId(igAccountId), id);
        return ResponseEntity.ok(ApiResponse.ok("Automation deleted."));
    }
}