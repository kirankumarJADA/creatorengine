package com.creatorengine.automation.deadletter.controller;

import com.creatorengine.automation.deadletter.DeadLetterService;
import com.creatorengine.automation.deadletter.dto.FailedJobResponse;
import com.creatorengine.automation.deadletter.entity.FailedJob;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/failed-jobs")
@Tag(name = "Failed Jobs", description = "Dead-letter queue management")
public class FailedJobController {

    private final DeadLetterService deadLetterService;
    private final JobQueue jobQueue;

    public FailedJobController(DeadLetterService deadLetterService, JobQueue jobQueue) {
        this.deadLetterService = deadLetterService;
        this.jobQueue = jobQueue;
    }

    @GetMapping
    @Operation(summary = "List failed jobs for the current user")
    public ResponseEntity<ApiResponse<List<FailedJobResponse>>> list(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        List<FailedJob> jobs = igAccountId != null && !igAccountId.isBlank()
                ? deadLetterService.listForAccount(uid, igAccountId.trim())
                : deadLetterService.listForUser(uid);

        List<FailedJobResponse> response = jobs.stream()
                .map(FailedJobResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed job")
    public ResponseEntity<ApiResponse<Void>> retry(
            @PathVariable String id,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        FailedJob job = deadLetterService.findById(uid, igAccountId, id)
                .orElseThrow(() -> new ResourceNotFoundException("FailedJob", id));

        AutomationJob retryJob = AutomationJob.fresh(uid, null, job.getAutomationId())
                .withIgAccountId(igAccountId);
        jobQueue.enqueue(retryJob);
        deadLetterService.deleteById(uid, igAccountId, id);

        return ResponseEntity.ok(ApiResponse.ok("Job queued for retry."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a failed job")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        deadLetterService.findById(uid, igAccountId, id)
                .orElseThrow(() -> new ResourceNotFoundException("FailedJob", id));
        deadLetterService.deleteById(uid, igAccountId, id);
        return ResponseEntity.ok(ApiResponse.ok("Failed job deleted."));
    }
}