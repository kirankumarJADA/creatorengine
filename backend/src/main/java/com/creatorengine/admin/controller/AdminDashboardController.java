package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminLogResponse;
import com.creatorengine.admin.dto.DashboardStatsResponse;
import com.creatorengine.auth.entity.User;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.automation.deadletter.FailedJobRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final AutomationRepository automationRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final FailedJobRepository failedJobRepository;
    private final InstagramAccountRepository instagramAccountRepository;

    public AdminDashboardController(
            UserRepository userRepository,
            AutomationRepository automationRepository,
            ExecutionLogRepository executionLogRepository,
            FailedJobRepository failedJobRepository,
            InstagramAccountRepository instagramAccountRepository
    ) {
        this.userRepository = userRepository;
        this.automationRepository = automationRepository;
        this.executionLogRepository = executionLogRepository;
        this.failedJobRepository = failedJobRepository;
        this.instagramAccountRepository = instagramAccountRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> stats() {
        List<User> users = userRepository.findAll();
        Instant now = Instant.now();
        Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        long totalUsers = users.size();
        long activeToday = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(startOfToday))
                .count();
        long active7d = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(sevenDaysAgo))
                .count();

        // Build uid -> email lookup once, reused below
        Map<String, String> emailByUid = new HashMap<>();
        for (User u : users) emailByUid.put(u.getUid(), u.getEmail());

        long igConnected = instagramAccountRepository.findAll().stream()
                .filter(o -> o.account() != null && o .account().getConnected())
                .count();

        var allAutomations = automationRepository.findAllAcrossUsers();
        long totalAutomations = allAutomations.size();
        long activeAutomations = allAutomations.stream()
                .filter(o -> o.automation().getEnabled())
                .count();
        long totalDmsSent = allAutomations.stream()
                .mapToLong(o -> o.automation().getSuccessCount())
                .sum();

        long failedJobsCount = failedJobRepository.listAllAcrossUsers(500).size();

        List<AdminLogResponse> recentActivity = executionLogRepository.listAllAcrossUsers(10).stream()
                .map(o -> AdminLogResponse.from(o.uid(), emailByUid.get(o.uid()), o.log()))
                .toList();

        DashboardStatsResponse response = new DashboardStatsResponse(
                totalUsers,
                activeToday,
                active7d,
                igConnected,
                totalAutomations,
                activeAutomations,
                totalDmsSent,
                failedJobsCount,
                recentActivity
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}