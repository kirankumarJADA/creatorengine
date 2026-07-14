package com.creatorengine.instagram.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.IceBreakerService;
import com.creatorengine.instagram.service.IceBreakerService.IceBreakerQuestion;
import com.creatorengine.instagram.service.InstagramAccountService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ice-breakers")
@Tag(name = "Ice Breakers", description = "Manage Instagram DM ice breaker questions")
public class IceBreakerController {

    private static final Logger log = LoggerFactory.getLogger(IceBreakerController.class);

    private final IceBreakerService iceBreakerService;
    private final InstagramAccountService accountService;

    public IceBreakerController(
            IceBreakerService iceBreakerService,
            InstagramAccountService accountService
    ) {
        this.iceBreakerService = iceBreakerService;
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "Get current ice breakers from Instagram")
    public ResponseEntity<ApiResponse<List<IceBreakerQuestion>>> get(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        List<IceBreakerQuestion> questions = iceBreakerService.fetch(account);
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }

    @PutMapping
    @Operation(summary = "Save ice breakers to Instagram (up to 4)")
    public ResponseEntity<ApiResponse<Void>> save(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId,
            @RequestBody List<IceBreakerQuestion> questions
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        try {
            iceBreakerService.save(account, questions);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception ex) {
            log.warn("Ice breaker save error for uid={}: {}", uid, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    @DeleteMapping
    @Operation(summary = "Clear all ice breakers from Instagram")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        InstagramAccount account = resolveAccount(uid, igAccountId);
        iceBreakerService.delete(account);
        return ResponseEntity.ok(ApiResponse.ok(null));
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
