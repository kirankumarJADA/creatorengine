package com.creatorengine.aifaq.controller;

import com.creatorengine.aifaq.dto.AiFaqConfigResponse;
import com.creatorengine.aifaq.dto.AiFaqTestRequest;
import com.creatorengine.aifaq.dto.AiFaqTestResponse;
import com.creatorengine.aifaq.entity.AiFaqConfig;
import com.creatorengine.aifaq.service.AiFaqService;
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
@RequestMapping("/api/ai-faq")
@Tag(name = "AI FAQ", description = "Pro feature: Gemini answers DMs that don't match a keyword automation")
public class AiFaqController {

    private static final Logger log = LoggerFactory.getLogger(AiFaqController.class);

    private final AiFaqService aiFaqService;
    private final InstagramAccountService accountService;
    private final PlanService planService;

    public AiFaqController(
            AiFaqService aiFaqService,
            InstagramAccountService accountService,
            PlanService planService
    ) {
        this.aiFaqService = aiFaqService;
        this.accountService = accountService;
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Get the current AI FAQ configuration")
    public ResponseEntity<ApiResponse<AiFaqConfigResponse>> get(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        Plan plan = planService.getPlan(uid);

        AiFaqConfig config = aiFaqService.fetch(uid, account.getInstagramUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                AiFaqConfigResponse.from(config, plan.isProOrHigher(), plan.name())));
    }

    @PutMapping
    @Operation(summary = "Save the AI FAQ configuration (Pro/Agency plans only)")
    public ResponseEntity<ApiResponse<AiFaqConfigResponse>> save(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId,
            @RequestBody AiFaqConfig incoming
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        Plan plan = planService.getPlan(uid);

        if (!plan.isProOrHigher()) {
            log.info("AI FAQ save blocked uid={} - plan {} not eligible.", uid, plan);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Upgrade to Pro to use AI FAQ."));
        }

        AiFaqConfig saved = aiFaqService.save(uid, account.getInstagramUserId(), incoming);
        return ResponseEntity.ok(ApiResponse.ok("AI FAQ settings saved.",
                AiFaqConfigResponse.from(saved, true, plan.name())));
    }

    @PostMapping("/test")
    @Operation(summary = "Test the draft AI FAQ config against a sample question (Pro/Agency, never sends a real DM)")
    public ResponseEntity<ApiResponse<AiFaqTestResponse>> test(@RequestBody AiFaqTestRequest req) {
        String uid = SecurityUtils.getCurrentUserId();
        Plan plan = planService.getPlan(uid);

        if (!plan.isProOrHigher()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Upgrade to Pro to test AI FAQ."));
        }

        try {
            String answer = aiFaqService.testAnswer(uid, req.toDraftConfig(), req.message());
            return ResponseEntity.ok(ApiResponse.ok(new AiFaqTestResponse(answer)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.warn("AI FAQ test failed uid={}: {}", uid, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error("AI request failed: " + ex.getMessage()));
        }
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
