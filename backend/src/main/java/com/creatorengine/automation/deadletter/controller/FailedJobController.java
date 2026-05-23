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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Failed-jobs REST surface.
 *
 * <ul>
 *   <li>{@code GET    /api/failed-jobs}            — list (up to 500 most-recent)</li>
 *   <li>{@code POST   /api/failed-jobs/{id}/retry} — re-enqueue and delete the row</li>
 *   <li>{@code DELETE /api/failed-jobs/{id}}       — discard without retrying</li>
 * </ul>
 *
 * <p>Auth is the standard JWT path; every operation is scoped to the
 * current user, so users can never see or act on each other's failures.</p>
 *
 * <p>Retry semantics: a successful retry deletes the historical row.
 * If the retry itself dead-letters again (after the engine's 3
 * attempts), a fresh row will appear — so the page always reflects
 * "what's currently broken". Keeping retried rows around would force
 * the UI to grow a status field ("Open / Retried") that the spec
 * doesn't ask for.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/failed-jobs")
@RequiredArgsConstructor
@Tag(name = "Failed Jobs", description = "Inspect and act on dead-lettered automation runs")
public class FailedJobController {

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 200;

    private final FailedJobRepository repository;
    private final JobQueue jobQueue;

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

        // Reconstruct the event from the snapshot. If we can't (legacy
        // row written before snapshots were captured, or an EventType
        // we no longer recognise), refuse rather than silently no-op.
        if (row.getEvent() == null) {
            throw new BadRequestException(
                    "This failed job predates retry support — its source event isn't recoverable.");
        }
        WebhookEventDto event = row.getEvent().toDto();
        if (event == null) {
            throw new BadRequestException(
                    "Failed job's snapshot couldn't be rehydrated (unknown event type).");
        }
        if (row.getAutomationId() == null || row.getAutomationId().isBlank()) {
            throw new BadRequestException("Failed job has no automation to retry against.");
        }

        // Build a fresh job — new id, attempt=1, no carryover error.
        AutomationJob fresh = AutomationJob.fresh(uid, event, row.getAutomationId());
        jobQueue.enqueue(fresh);

        // Successful enqueue → row goes away. Engine will re-write it
        // only if this retry exhausts its own attempts.
        repository.delete(uid, id);

        log.info("Retry enqueued uid={} failedJobId={} → new jobId={}",
                uid, id, fresh.jobId());
        return ResponseEntity.ok(ApiResponse.ok(
                "Retry queued.",
                Map.of("jobId", fresh.jobId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Discard a failed job without retrying")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        String uid = SecurityUtils.getCurrentUserId();
        // Probe first so a missing row returns 404 rather than a 200 no-op.
        repository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Failed job not found."));
        repository.delete(uid, id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Failed job deleted.", null));
    }
}
