package com.creatorengine.autopilot.controller;

import com.creatorengine.autopilot.dto.AutopilotConfigResponse;
import com.creatorengine.autopilot.dto.AutopilotStatsResponse;
import com.creatorengine.autopilot.entity.AutopilotConfig;
import com.creatorengine.autopilot.service.AutopilotService;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.plan.entity.Plan;
import com.creatorengine.plan.service.PlanService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autopilot")
@Tag(name = "AI Autopilot", description = "Pro feature: full AI sales/support conversations, separate from AI FAQ")
public class AutopilotController {

    private static final Logger log = LoggerFactory.getLogger(AutopilotController.class);

    private final AutopilotService autopilotService;
    private final InstagramAccountService accountService;
    private final PlanService planService;

    public AutopilotController(
            AutopilotService autopilotService,
            InstagramAccountService accountService,
            PlanService planService
    ) {
        this.autopilotService = autopilotService;
        this.accountService = accountService;
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Get the current AI Autopilot configuration")
    public ResponseEntity<ApiResponse<AutopilotConfigResponse>> get(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        Plan plan = planService.getPlan(uid);

        AutopilotConfig config = autopilotService.fetch(uid, account.getInstagramUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                AutopilotConfigResponse.from(config, plan.isProOrHigher(), plan.name())));
    }

    @PutMapping
    @Operation(summary = "Save the AI Autopilot configuration (Pro/Agency plans only)")
    public ResponseEntity<ApiResponse<AutopilotConfigResponse>> save(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId,
            @RequestBody AutopilotConfig incoming
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        Plan plan = planService.getPlan(uid);

        if (!plan.isProOrHigher()) {
            log.info("Autopilot save blocked uid={} - plan {} not eligible.", uid, plan);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Upgrade to Pro to use AI Autopilot."));
        }

        AutopilotConfig saved = autopilotService.save(uid, account.getInstagramUserId(), incoming);
        return ResponseEntity.ok(ApiResponse.ok("AI Autopilot settings saved.",
                AutopilotConfigResponse.from(saved, true, plan.name())));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get AI Autopilot usage stats for the active account (Pro/Agency plans only)")
    public ResponseEntity<ApiResponse<AutopilotStatsResponse>> stats(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        Plan plan = planService.getPlan(uid);

        if (!plan.isProOrHigher()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Upgrade to Pro to view AI Autopilot stats."));
        }

        AutopilotService.AutopilotStats s = autopilotService.stats(uid, account.getInstagramUserId());
        return ResponseEntity.ok(ApiResponse.ok(new AutopilotStatsResponse(
                s.enabled(), "AI (free tier)", s.conversationCount(), s.avgResponseTimeMs(),
                s.contactsHandled(), s.escalations(), s.qualifiedLeads()
        )));
    }

    private InstagramAccount resolveAccount(String uid, String igAccountId) {
        if (igAccountId != null && !igAccountId.isBlank()) {
            return accountService.findByIgId(uid, igAccountId)
                    .orElseThrow(() -> new RuntimeException("Instagram account not found."));
        }
        return accountService.find(uid)
                .orElseThrow(() -> new RuntimeException("No Instagram account connected."));
    }
}
