package com.creatorengine.automation.controller;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.dto.ToggleRequest;
import com.creatorengine.automation.service.AutomationService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/automations")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AutomationResponse>>> list() {
        String uid = SecurityUtils.getCurrentUserId();
        List<AutomationResponse> automations = automationService.listForUser(uid);
        return ResponseEntity.ok(ApiResponse.ok(automations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> get(@PathVariable String id) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse automation = automationService.get(uid, id);
        return ResponseEntity.ok(ApiResponse.ok(automation));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AutomationResponse>> create(
            @Valid @RequestBody AutomationRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse created = automationService.create(uid, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Automation created.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody AutomationRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse updated = automationService.update(uid, id, req);
        return ResponseEntity.ok(ApiResponse.ok("Automation updated.", updated));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AutomationResponse>> toggle(
            @PathVariable String id,
            @Valid @RequestBody ToggleRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse toggled = automationService.toggle(uid, id, req.enabled());
        return ResponseEntity.ok(ApiResponse.ok(
                req.enabled() ? "Automation enabled." : "Automation disabled.",
                toggled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        String uid = SecurityUtils.getCurrentUserId();
        automationService.delete(uid, id);
        return ResponseEntity.ok(ApiResponse.ok("Automation deleted."));
    }
}