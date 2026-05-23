package com.creatorengine.automation.logger.controller;

import com.creatorengine.automation.logger.dto.LogResponse;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code GET /api/logs} — recent execution-log rows for the current user.
 *
 * <p>Returns up to 500 rows (capped by {@link ExecutionLogRepository#listForUser})
 * sorted timestamp-DESC. Search / status / automation / date filters
 * happen on the frontend, mirroring the Contacts page pattern. The
 * 500-row cap is a deliberate MVP choice — once a user crosses that
 * volume regularly we switch to cursor pagination here (and add a
 * Firestore index on {@code (timestamp DESC)} which Firestore should
 * auto-create on first read anyway).</p>
 *
 * <p>Auth: standard JWT. The repository scopes every read to the
 * resolved UID, so users never see anyone else's logs.</p>
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Read-only activity log feed")
public class LogController {

    /** Hard cap on rows we return — see class doc for rationale. */
    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 200;

    private final ExecutionLogRepository repository;

    @GetMapping
    @Operation(summary = "List recent activity log rows for the current user")
    public ResponseEntity<ApiResponse<List<LogResponse>>> list(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        int requested = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        List<LogResponse> items = repository.listForUser(uid, requested).stream()
                .map(LogResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }
}
