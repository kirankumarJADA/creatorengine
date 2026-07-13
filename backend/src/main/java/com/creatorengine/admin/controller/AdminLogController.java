package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminLogResponse;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    private final ExecutionLogRepository executionLogRepository;
    private final UserRepository userRepository;

    public AdminLogController(
            ExecutionLogRepository executionLogRepository,
            UserRepository userRepository
    ) {
        this.executionLogRepository = executionLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLogResponse>>> list(
            @RequestParam(required = false) Integer limit
    ) {
        int requested = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        Map<String, String> emailByUid = new HashMap<>();
        userRepository.findAll().forEach(u -> emailByUid.put(u.getUid(), u.getEmail()));

        List<AdminLogResponse> result = executionLogRepository.listAllAcrossUsers(requested).stream()
                .map(o -> AdminLogResponse.from(o.uid(), emailByUid.get(o.uid()), o.log()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}