package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminUserResponse;
import com.creatorengine.auth.entity.User;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import com.creatorengine.plan.entity.Plan;
import com.creatorengine.plan.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserRepository userRepository;
    private final InstagramAccountRepository instagramAccountRepository;
    private final PlanService planService;

    public AdminUserController(
            UserRepository userRepository,
            InstagramAccountRepository instagramAccountRepository,
            PlanService planService
    ) {
        this.userRepository = userRepository;
        this.instagramAccountRepository = instagramAccountRepository;
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> list() {
        List<AdminUserResponse> result = userRepository.findAll().stream()
                .map(u -> {
                    Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(u.getUid());
                    boolean connected = ig.map(InstagramAccount::getConnected).orElse(false);
                    String username = ig.map(InstagramAccount::getUsername).orElse(null);
                    Plan plan = planService.getPlan(u.getUid());
                    return AdminUserResponse.from(u, connected, username, plan);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{uid}/enable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> enable(@PathVariable String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        user.setEnabled(true);
        userRepository.save(user);

        Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(uid);
        return ResponseEntity.ok(ApiResponse.ok("User enabled.",
                AdminUserResponse.from(user,
                        ig.map(InstagramAccount::getConnected).orElse(false),
                        ig.map(InstagramAccount::getUsername).orElse(null),
                        planService.getPlan(uid))));
    }

    @PatchMapping("/{uid}/disable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> disable(@PathVariable String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        user.setEnabled(false);
        userRepository.save(user);

        Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(uid);
        return ResponseEntity.ok(ApiResponse.ok("User disabled.",
                AdminUserResponse.from(user,
                        ig.map(InstagramAccount::getConnected).orElse(false),
                        ig.map(InstagramAccount::getUsername).orElse(null),
                        planService.getPlan(uid))));
    }

    /**
     * Manually set a user's plan. No payment system yet — this is how you
     * (the admin) flip your own account to PRO to test AI features before
     * Stripe billing is wired up.
     * Body: {"plan": "PRO"} — one of FREE, PRO, AGENCY.
     */
    @PatchMapping("/{uid}/plan")
    public ResponseEntity<ApiResponse<AdminUserResponse>> setPlan(
            @PathVariable String uid,
            @RequestBody Map<String, String> body
    ) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));

        String planName = body.get("plan");
        Plan plan;
        try {
            plan = Plan.valueOf(planName == null ? "" : planName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid plan. Use one of: FREE, PRO, AGENCY."));
        }

        planService.setPlan(uid, plan);
        log.info("Admin set plan={} for uid={}", plan, uid);

        Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(uid);
        return ResponseEntity.ok(ApiResponse.ok("Plan updated to " + plan.displayName() + ".",
                AdminUserResponse.from(user,
                        ig.map(InstagramAccount::getConnected).orElse(false),
                        ig.map(InstagramAccount::getUsername).orElse(null),
                        plan)));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uid) {
        userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));

        userRepository.deleteById(uid);
        return ResponseEntity.ok(ApiResponse.<Void>ok("User deleted.", null));
    }
}