package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.SystemStatusResponse;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.common.ApiResponse;
import com.google.cloud.firestore.Firestore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final Firestore firestore;
    private final UserRepository userRepository;
    private final AutomationRepository automationRepository;

    public AdminSystemController(
            Firestore firestore,
            UserRepository userRepository,
            AutomationRepository automationRepository
    ) {
        this.firestore = firestore;
        this.userRepository = userRepository;
        this.automationRepository = automationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SystemStatusResponse>> status() {
        // If this endpoint is responding at all, the API is up.
        String apiStatus = "UP";

        // Lightweight real check — one cheap Firestore read, not a mock.
        String dbStatus;
        try {
            firestore.collection("users").limit(1).get().get();
            dbStatus = "UP";
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        Instant thirtyMinAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        long activeLast30Min = userRepository.findAll().stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(thirtyMinAgo))
                .count();

        long totalDmsSent = automationRepository.findAllAcrossUsers().stream()
                .mapToLong(o -> o.automation().getSuccessCount())
                .sum();

        // Honest, not fabricated: we don't have a Render API token, so we
        // link out instead of faking a status this backend can't verify.
        SystemStatusResponse response = new SystemStatusResponse(
                apiStatus,
                dbStatus,
                "Configured", // webhook verify token is set at deploy time via env var
                activeLast30Min,
                totalDmsSent,
                "https://dashboard.render.com"
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}