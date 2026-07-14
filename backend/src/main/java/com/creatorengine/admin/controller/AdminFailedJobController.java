package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminFailedJobResponse;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.automation.deadletter.FailedJob;
import com.creatorengine.automation.deadletter.FailedJobRepository;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.instagram.dto.WebhookEventDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/failed-jobs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFailedJobController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    private final FailedJobRepository failedJobRepository;
    private final UserRepository userRepository;
    private final JobQueue jobQueue;

    public AdminFailedJobController(
            FailedJobRepository failedJobRepository,
            UserRepository userRepository,
            JobQueue jobQueue
    ) {
        this.failedJobRepository = failedJobRepository;
        this.userRepository = userRepository;
        this.jobQueue = jobQueue;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminFailedJobResponse>>> list(
            @RequestParam(required = false) Integer limit
    ) {
        int requested = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        Map<String, String> emailByUid = new HashMap<>();
        userRepository.findAll().forEach(u -> emailByUid.put(u.getUid(), u.getEmail()));

        List<AdminFailedJobResponse> result = failedJobRepository.listAllAcrossUsers(requested).stream()
                .map(o -> AdminFailedJobResponse.from(o.uid(), emailByUid.get(o.uid()), o.job()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{uid}/{id}/retry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retry(
            @PathVariable String uid, @PathVariable String id
    ) {
        FailedJob row = failedJobRepository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Failed job not found."));

        if (row.getEvent() == null) {
            throw new BadRequestException("This failed job predates retry support.");
        }

        WebhookEventDto event = row.getEvent().toDto();
        if (event == null) {
            throw new BadRequestException("Failed job's snapshot couldn't be rehydrated.");
        }

        if (row.getAutomationId() == null || row.getAutomationId().isBlank()) {
            throw new BadRequestException("Failed job has no automation to retry against.");
        }

        AutomationJob fresh = AutomationJob.fresh(uid, event, row.getAutomationId())
                .withIgAccountId(event.receivingAccountId());
        jobQueue.enqueue(fresh);
        failedJobRepository.delete(uid, id);

        return ResponseEntity.ok(ApiResponse.ok("Retry queued.", Map.of("jobId", fresh.jobId())));
    }

    @DeleteMapping("/{uid}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String uid, @PathVariable String id
    ) {
        failedJobRepository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Failed job not found."));

        failedJobRepository.delete(uid, id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Failed job deleted.", null));
    }
}