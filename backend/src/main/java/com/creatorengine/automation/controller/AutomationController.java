package com.creatorengine.automation.controller;

import com.creatorengine.automation.dto.AutomationRequest;
import com.creatorengine.automation.dto.AutomationResponse;
import com.creatorengine.automation.dto.ToggleRequest;
import com.creatorengine.automation.service.AutomationService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for managing automations.
 *
 * <p>Paths intentionally live at {@code /api/automations} (not
 * {@code /api/v1/...}) to match the spec — this matches the auth
 * module's path style as well.</p>
 *
 * <p>Every endpoint requires authentication; the resolved Firebase
 * UID becomes the owner of all operations via
 * {@link SecurityUtils#getCurrentUserId()}.</p>
 */
@RestController
@RequestMapping("/api/automations")
@RequiredArgsConstructor
@Tag(name = "Automations", description = "Automation CRUD endpoints")
public class AutomationController {

    private final AutomationService service;

    @GetMapping
    @Operation(summary = "List all automations for the current user")
    public ResponseEntity<ApiResponse<List<AutomationResponse>>> list() {
        String uid = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(service.listForUser(uid)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single automation by id")
    public ResponseEntity<ApiResponse<AutomationResponse>> get(@PathVariable String id) {
        String uid = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(service.get(uid, id)));
    }

    @PostMapping
    @Operation(summary = "Create a new automation")
    public ResponseEntity<ApiResponse<AutomationResponse>> create(
            @Valid @RequestBody AutomationRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        AutomationResponse created = service.create(uid, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Automation created.", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing automation")
    public ResponseEntity<ApiResponse<AutomationResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody AutomationRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                "Automation updated.", service.update(uid, id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an automation")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        String uid = SecurityUtils.getCurrentUserId();
        service.delete(uid, id);
        return ResponseEntity.ok(ApiResponse.ok("Automation deleted."));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Enable or pause an automation")
    public ResponseEntity<ApiResponse<AutomationResponse>> toggle(
            @PathVariable String id,
            @Valid @RequestBody ToggleRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                "Automation toggled.", service.toggle(uid, id, req.enabled())));
    }
}
