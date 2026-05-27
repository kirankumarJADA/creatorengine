package com.creatorengine.automation.deadletter.controller;

import com.creatorengine.automation.deadletter.FailedJob;
import com.creatorengine.automation.deadletter.FailedJobRepository;
import com.creatorengine.automation.deadletter.dto.FailedJobResponse;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/failed-jobs")
@Tag(name = "Failed Jobs", description = "Inspect and act on dead-lettered automation runs")
public class FailedJobController {

    private static final Logger log = LoggerFactory.getLogger(FailedJobController.class);

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 200;

    private final FailedJobRepository repository;
    private final JobQueue jobQueue;

    public FailedJobController(FailedJobRepository repository, JobQueue jobQueue) {
        this.repository = repository;
        this.jobQueue = jobQueue;
    }

    @GetMapping
    @Operation(summary = "List recent failed jobs for the current user")
    public ResponseEntity<ApiResponse<List<FailedJobResponse>>> list(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        int requested = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        List<FailedJobResponse> items = repository.listForUser(uid, requested).stream()
                .map(FailedJobResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Re-enqueue a failed job and delete the historical row")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retry(@PathVariable("id") String id) {
        String uid = SecurityUtils.getCurrentUserId();

        FailedJob row = repository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Failed job not found."));

        if (row.getEvent() == null) {
            throw new BadRequestException(
                    "This failed job predates retry support - its source event isn't recoverable.");
        }

        WebhookEventDto event = row.getEvent().toDto();
        if (event == null) {
            throw new BadRequestException(
                    "Failed job's snapshot couldn't be rehydrated (unknown event type).");
        }

        if (row.getAutomationId() == null || row.getAutomationId().isBlank()) {
            throw new BadRequestException("Failed job has no automation to retry against.");
        }

        AutomationJob fresh = AutomationJob.fresh(uid, event, row.getAutomationId());
        jobQueue.enqueue(fresh);
        repository.delete(uid, id);

        log.info("Retry enqueued uid={} failedJobId={} newJobId={}", uid, id, fresh.jobId());

        return ResponseEntity.ok(ApiResponse.ok(
                "Retry queued.",
                Map.of("jobId", fresh.jobId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Discard a failed job without retrying")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        String uid = SecurityUtils.getCurrentUserId();

        repository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Failed job not found."));

        repository.delete(uid, id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Failed job deleted.", null));
    }
}