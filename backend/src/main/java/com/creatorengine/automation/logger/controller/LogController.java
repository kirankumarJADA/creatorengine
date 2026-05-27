package com.creatorengine.automation.logger.controller;

import com.creatorengine.automation.logger.dto.LogResponse;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Activity Logs", description = "Read-only activity log feed")
public class LogController {

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 200;

    private final ExecutionLogRepository repository;

    public LogController(ExecutionLogRepository repository) {
        this.repository = repository;
    }

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