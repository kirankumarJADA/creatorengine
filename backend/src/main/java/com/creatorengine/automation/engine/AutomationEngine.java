package com.creatorengine.automation.engine;

import com.creatorengine.automation.cooldown.CooldownService;
import com.creatorengine.automation.deadletter.DeadLetterService;
import com.creatorengine.automation.dedup.EventDeduplicationService;
import com.creatorengine.automation.entity.ActionType;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.executor.ActionExecutor;
import com.creatorengine.automation.executor.ExecutionContext;
import com.creatorengine.automation.executor.ExecutionResult;
import com.creatorengine.automation.logger.ExecutionLogger;
import com.creatorengine.automation.matcher.AutomationMatcher;
import com.creatorengine.automation.matcher.ConditionEvaluator;
import com.creatorengine.automation.matcher.ConditionEvaluator.Result;
import com.creatorengine.automation.queue.AutomationJob;
import com.creatorengine.automation.queue.JobQueue;
import com.creatorengine.automation.ratelimit.RateLimitService;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.automation.retry.RetryPolicy;
import com.creatorengine.instagram.dto.WebhookEventDto;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * The automation execution engine — split into two halves to
 * accommodate the queue:
 *
 * <pre>
 *  dispatch(uid, event)          — called from WebhookService, synchronous
 *  ├─ dedup check                  → skip event if already processed
 *  ├─ matcher.findCandidates      → trigger + enabled filter
 *  ├─ evaluator.evaluate          → keyword + match-type filter
 *  ├─ cooldownService.canFire     → anti-spam guard
 *  └─ queue.enqueue(job)          → one job per (event × automation)
 *
 *  processJob(job)               — called from QueueWorker, runs on worker thread
 *  ├─ re-fetch automation         → respects mid-flight disable
 *  ├─ rateLimitService.tryAcquire → defer with backoff if hot
 *  ├─ executor.execute(ctx)       → the actual Meta DM
 *  ├─ logger.logMatch             → write to execution_logs
 *  ├─ cooldownService.recordFiring
 *  └─ retry / dead-letter on failure
 * </pre>
 *
 * <p>The {@code @Async} of the previous iteration is gone — async-ness
 * is now provided by the {@link com.creatorengine.automation.queue.QueueWorker}
 * pool, which is the right primitive for production (bounded, swappable
 * for Redis/SQS, can rate-limit cleanly).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngine {

    // Matching / evaluation
    private final AutomationMatcher matcher;
    private final ConditionEvaluator evaluator;

    // Reliability layers
    private final EventDeduplicationService dedupService;
    private final CooldownService cooldownService;
    private final RateLimitService rateLimitService;
    private final RetryPolicy retryPolicy;
    private final DeadLetterService deadLetterService;

    // Execution
    private final ActionExecutor executor;
    private final ExecutionLogger logger;

    // Persistence + lookups
    private final AutomationRepository automationRepository;
    private final InstagramAccountService instagramAccountService;

    // Queue
    private final JobQueue queue;

    // ─── Dispatch (called from WebhookService) ──────────────
    /**
     * Process one incoming webhook event:
     * dedup → match → evaluate → cooldown → enqueue.
     *
     * <p>Synchronous and fast — the work itself happens on the worker
     * pool. Returns nothing because the webhook only cares whether
     * it accepted the call, not what the engine ultimately decides.</p>
     */
    public void dispatch(String uid, WebhookEventDto event) {
        if (uid == null || event == null) {
            log.warn("dispatch called with missing uid or event.");
            return;
        }

        // 1. Dedup: have we already processed this event id?
        if (!dedupService.markIfNew(uid, event)) {
            logger.logDuplicateIgnored(uid, event);
            return;
        }

        // 2. Find candidates by trigger + enabled.
        List<Automation> candidates = matcher.findCandidates(uid, event);
        if (candidates.isEmpty()) {
            log.debug("No matching automations uid={} event={}", uid, event.type());
            return;
        }

        // 3. Per-automation gating + enqueue.
        for (Automation automation : candidates) {
            Result conditionResult = evaluator.evaluate(automation.getCondition(), event);
            if (!conditionResult.matched()) {
                log.debug("Automation {} skipped: {}",
                        automation.getId(), conditionResult.reason());
                continue;
            }
            if (!cooldownService.canFire(uid, automation, event)) {
                // Note: cooldown miss is a silent skip for the user, but we
                // *do* record it as a log entry so the Activity Logs page can
                // surface "this automation got muffled by your cooldown".
                logger.logCooldownSkipped(uid, automation, event);
                continue;
            }

            AutomationJob job = AutomationJob.fresh(uid, event, automation.getId());
            queue.enqueue(job);
            log.debug("Enqueued job {} for automation {}", job.jobId(), automation.getId());
        }
    }

    // ─── Per-job processing (called from QueueWorker) ───────
    /**
     * Run one or more sequential actions from the automation's
     * effective chain, starting at {@code job.actionIndex()}.
     *
     * <p>Three queue transitions can happen mid-chain:</p>
     * <ul>
     *   <li><b>DELAY</b> — enqueue a continuation pointing at the next
     *       action with {@code attempt=1}, then return the worker.</li>
     *   <li><b>Rate-limited</b> — re-enqueue the SAME action index
     *       after the rate-limiter's suggested backoff. Does NOT burn
     *       a retry attempt.</li>
     *   <li><b>Retryable failure</b> — re-enqueue the SAME action
     *       index with {@code attempt++}, up to {@link RetryPolicy#maxAttempts()}.</li>
     * </ul>
     *
     * <p>The action counter resets to 1 when we advance to the next
     * action (each action gets its own three-strike budget). Cooldown
     * is recorded on the first user-visible success per invocation —
     * partial-chain progress still respects the cooldown so spam
     * triggers don't keep re-firing partially-broken chains.</p>
     */
    public void processJob(AutomationJob job) {
        if (job == null || job.uid() == null || job.event() == null
                || job.automationId() == null) {
            log.warn("Worker got malformed job, skipping.");
            return;
        }

        // 1. Re-fetch the automation — it could have been disabled or
        //    deleted while the job was queued.
        Optional<Automation> automationOpt =
                automationRepository.findById(job.uid(), job.automationId());
        if (automationOpt.isEmpty()) {
            log.info("Skipping job {} — automation {} no longer exists",
                    job.jobId(), job.automationId());
            return;
        }
        Automation automation = automationOpt.get();
        if (!automation.isEnabled()) {
            log.info("Skipping job {} — automation {} was disabled",
                    job.jobId(), job.automationId());
            return;
        }

        java.util.List<Automation.Action> actions = automation.getEffectiveActions();
        if (actions.isEmpty()) {
            log.warn("Skipping job {} — automation {} has no actions", job.jobId(), job.automationId());
            return;
        }
        if (job.actionIndex() >= actions.size()) {
            // A continuation arrived but the chain was shortened by
            // an edit. Treat as "nothing more to do" — silent success.
            log.info("Job {} actionIndex {} ≥ chain length {} — chain complete.",
                    job.jobId(), job.actionIndex(), actions.size());
            return;
        }

        // 2. Re-fetch connected account once per invocation.
        InstagramAccount account = instagramAccountService.find(job.uid()).orElse(null);
        String rateKey = account != null ? account.getInstagramUserId() : null;

        int currentAttempt = Math.max(1, job.attempt());
        boolean firedThisInvocation = false;

        // 3. Walk the chain from the resumed index.
        for (int i = job.actionIndex(); i < actions.size(); i++) {
            Automation.Action action = actions.get(i);
            if (action == null || action.getType() == null) {
                log.warn("Job {} skipping null action at index {}", job.jobId(), i);
                continue;
            }

            // ─── DELAY: hand off to the queue, free the worker ───────
            if (action.getType() == ActionType.DELAY) {
                Integer secs = action.getDelaySeconds();
                Duration wait = secs == null
                        ? Duration.ZERO
                        : Duration.ofSeconds(Math.max(0, secs));
                AutomationJob continuation = job.advanceTo(i + 1);
                queue.enqueueDelayed(continuation, wait);
                log.info("Job {} hit DELAY at step {} for {}s — continuation enqueued",
                        job.jobId(), i + 1, wait.toSeconds());
                return;
            }

            // ─── Rate-limit check before EACH send-type action ───────
            // SAVE_CONTACT doesn't hit Meta, so don't waste budget on it.
            boolean isSend = action.getType() != ActionType.SAVE_CONTACT;
            if (isSend && rateKey != null && !rateLimitService.tryAcquire(rateKey)) {
                Duration backoff = rateLimitService.suggestedBackoff(rateKey);
                if (backoff.isZero() || backoff.isNegative()) {
                    backoff = Duration.ofSeconds(5);
                }
                // Re-enqueue at THIS action; same attempt, same index.
                AutomationJob deferred = job
                        .withActionIndex(i)
                        .withAttempt(currentAttempt);
                queue.enqueueDelayed(deferred, backoff);
                log.info("Job {} rate-limited at step {}, deferring by {}s",
                        job.jobId(), i + 1, backoff.toSeconds());
                return;
            }

            // ─── Execute the action ──────────────────────────────────
            ExecutionContext ctx = new ExecutionContext(job.uid(), automation, job.event(), account);
            ExecutionResult result;
            try {
                result = executor.execute(ctx, action);
            } catch (Exception ex) {
                log.error("Unexpected error executing job {} at step {}: {}",
                        job.jobId(), i + 1, ex.getMessage(), ex);
                result = ExecutionResult.failed(null,
                        "Engine exception at step " + (i + 1) + ": " + ex.getMessage(), null);
            }

            // Log every executed action (success or failure).
            logger.logMatch(job.uid(), automation, job.event(), result);

            // ─── Outcome routing for this single action ─────────────
            boolean userVisibleSuccess =
                    result.messageSent() || isSaveContactSuccess(result, action);
            if (userVisibleSuccess) {
                if (!firedThisInvocation) {
                    cooldownService.recordFiring(job.uid(), automation, job.event());
                    firedThisInvocation = true;
                }
                // Reset attempt budget for the NEXT action and continue in-memory.
                currentAttempt = 1;
                continue;
            }

            // Not a success — retry or dead-letter.
            if (retryPolicy.isRetryable(result)) {
                int nextAttempt = currentAttempt + 1;
                if (nextAttempt <= retryPolicy.maxAttempts()) {
                    Duration delay = retryPolicy.backoffFor(nextAttempt);
                    AutomationJob retry = job
                            .withActionIndex(i)
                            .withAttempt(nextAttempt)
                            .withLastError(result.error());
                    queue.enqueueDelayed(retry, delay);
                    log.info("Job {} step {} failed retryably (attempt {}/{}), retrying in {}s: {}",
                            job.jobId(), i + 1, currentAttempt, retryPolicy.maxAttempts(),
                            delay.toSeconds(), result.error());
                    return;
                }
                deadLetterService.record(job.withActionIndex(i), automation,
                        "Max retries exceeded at step " + (i + 1) + ": " + result.error());
                return;
            }

            // Non-retryable — straight to dead-letter, no more actions run.
            deadLetterService.record(job.withActionIndex(i), automation,
                    "Non-retryable at step " + (i + 1) + ": " + result.error());
            return;
        }

        // Reached the end of the chain successfully (or with only no-op actions).
        log.info("Job {} chain complete ({} action(s))", job.jobId(), actions.size());
    }

    private boolean isSaveContactSuccess(ExecutionResult result, Automation.Action action) {
        // SAVE_CONTACT writes the contact but never sends a message;
        // treat as a successful firing for cooldown purposes.
        return result.error() == null
                && action.getType() == ActionType.SAVE_CONTACT;
    }
}
