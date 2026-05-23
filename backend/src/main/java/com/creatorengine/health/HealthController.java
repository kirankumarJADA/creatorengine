package com.creatorengine.health;

import com.creatorengine.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * {@code GET /api/health} — unauthenticated liveness/readiness endpoint.
 *
 * <p>Public on purpose so external monitors (UptimeRobot, Pingdom,
 * etc.) can hit it. The body reports infrastructure state without
 * leaking user data or tokens — see {@link HealthService} for what's
 * included.</p>
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health + readiness")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(summary = "Snapshot of queue, webhook, Firestore, and Meta health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(healthService.snapshot()));
    }
}
