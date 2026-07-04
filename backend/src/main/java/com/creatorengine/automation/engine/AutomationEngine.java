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
import com.creatorengine.instagram.entity.EventType;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.service.InstagramAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);

    private static final String FOLLOW_GATE_PREFIX = "fgate:";

    private final AutomationMatcher matcher;
    private final ConditionEvaluator evaluator;
    private final EventDeduplicationService dedupService;
    private final CooldownService cooldownService;
    private final RateLimitService rateLimitService;
    private final RetryPolicy retryPolicy;
    private final DeadLetterService deadLetterService;
    private final ActionExecutor executor;
    private final ExecutionLogger logger;
    private final AutomationRepository automationRepository;
    private final InstagramAccountService instagramAccountService;
    private final JobQueue queue;

    public AutomationEngine(
            AutomationMatcher matcher,
            ConditionEvaluator evaluator,
            EventDeduplicationService dedupService,
            CooldownService cooldownService,
            RateLimitService rateLimitService,
            RetryPolicy retryPolicy,
            DeadLetterService deadLetterService,
            ActionExecutor executor,
            ExecutionLogger logger,
            AutomationRepository automationRepository,
            InstagramAccountService instagramAccountService,
            JobQueue queue
    ) {
        this.matcher = matcher;
        this.evaluator = evaluator;
        this.dedupService = dedupService;
        this.cooldownService = cooldownService;
        this.rateLimitService = rateLimitService;
        this.retryPolicy = retryPolicy;
        this.deadLetterService = deadLetterService;
        this.executor = executor;
        this.logger = logger;
        this.automationRepository = automationRepository;
        this.instagramAccountService = instagramAccountService;
        this.queue = queue;
    }

    public void dispatch(String uid, WebhookEventDto event) {
        if (uid == null || event == null) {
            log.warn("dispatch called with missing uid or event.");
            return;
        }

        if (!dedupService.markIfNew(uid, event)) {
            logger.logDuplicateIgnored(uid, event);
            return;
        }

        if (event.type() == EventType.DM && isFollowGatePayload(event.quickReplyPayload())) {
            handleFollowGateCompletion(uid, event);
            return;
        }

        List<Automation> candidates = matcher.findCandidates(uid, event);
        if (candidates.isEmpty()) {
            log.debug("No matching automations uid={} event={}", uid, event.type());
            return;
        }

        for (Automation automation : candidates) {
            Result conditionResult = evaluator.evaluate(automation.getCondition(), event);
            if (!conditionResult.matched()) {
                log.debug("Automation {} skipped: {}", automation.getId(), conditionResult.reason());
                continue;
            }

            if (!cooldownService.canFire(uid, automation, event)) {
                logger.logCooldownSkipped(uid, automation, event);
                continue;
            }

            AutomationJob job = AutomationJob.fresh(uid, event, automation.getId());
            queue.enqueue(job);
            log.debug("Enqueued job {} for automation {}", job.jobId(), automation.getId());
        }
    }

    private static boolean isFollowGatePayload(String payload) {
        return payload != null && payload.startsWith(FOLLOW_GATE_PREFIX);
    }

    private void handleFollowGateCompletion(String uid, WebhookEventDto event) {
        String automationId = event.quickReplyPayload().substring(FOLLOW_GATE_PREFIX.length());
        if (automationId.isBlank()) {
            log.warn("Follow-gate payload had no automation id.");
            return;
        }

        Optional<Automation> automationOpt = automationRepository.findById(uid, automationId);
        if (automationOpt.isEmpty()) {
            log.info("Follow-gate completion: automation {} no longer exists.", automationId);
            return;
        }
        if (!automationOpt.get().getEnabled()) {
            log.info("Follow-gate completion: automation {} is disabled.", automationId);
            return;
        }

        AutomationJob job = AutomationJob.fresh(uid, event, automationId);
        queue.enqueue(job);
        log.info("Follow-gate completed for automation {} - delivering content.", automationId);
    }

    public void processJob(AutomationJob job) {
        if (job == null || job.uid() == null || job.event() == null || job.automationId() == null) {
            log.warn("Worker got malformed job, skipping.");
            return;
        }

        Optional<Automation> automationOpt =
                automationRepository.findById(job.uid(), job.automationId());

        if (automationOpt.isEmpty()) {
            log.info("Skipping job {} - automation {} no longer exists",
                    job.jobId(), job.automationId());
            return;
        }

        Automation automation = automationOpt.get();
        if (!automation.getEnabled()) {
            log.info("Skipping job {} - automation {} was disabled",
                    job.jobId(), job.automationId());
            return;
        }

        InstagramAccount account = instagramAccountService.find(job.uid()).orElse(null);

        if (automation.getFollowGateEnabled() && job.event().type() == EventType.COMMENT) {
            runFollowGateAsk(job, automation, account);
            return;
        }

        List<Automation.Action> actions = automation.getEffectiveActions();

        if (actions.isEmpty()) {
            if (automation.getPublicReplyEnabled()
                    && job.event().type() == EventType.COMMENT) {
                runPublicReplyOnly(job, automation, account);
            } else {
                log.warn("Skipping job {} - automation {} has no actions and no public reply",
                        job.jobId(), job.automationId());
            }
            return;
        }

        if (job.actionIndex() >= actions.size()) {
            log.info("Job {} actionIndex {} >= chain length {} - chain complete.",
                    job.jobId(), job.actionIndex(), actions.size());
            return;
        }

        String rateKey = account != null ? account.getInstagramUserId() : null;

        int currentAttempt = Math.max(1, job.attempt());
        boolean firedThisInvocation = false;

        for (int i = job.actionIndex(); i < actions.size(); i++) {
            Automation.Action action = actions.get(i);
            if (action == null || action.getType() == null) {
                log.warn("Job {} skipping null action at index {}", job.jobId(), i);
                continue;
            }

            if (action.getType() == ActionType.DELAY) {
                Integer secs = action.getDelaySeconds();
                Duration wait = secs == null
                        ? Duration.ZERO
                        : Duration.ofSeconds(Math.max(0, secs));

                AutomationJob continuation = job.advanceTo(i + 1);
                queue.enqueueDelayed(continuation, wait);

                log.info("Job {} hit DELAY at step {} for {}s - continuation enqueued",
                        job.jobId(), i + 1, wait.toSeconds());
                return;
            }

            boolean isSend = action.getType() != ActionType.SAVE_CONTACT;
            if (isSend && rateKey != null && !rateLimitService.tryAcquire(rateKey)) {
                Duration backoff = rateLimitService.suggestedBackoff(rateKey);
                if (backoff.isZero() || backoff.isNegative()) {
                    backoff = Duration.ofSeconds(5);
                }

                AutomationJob deferred = job
                        .withActionIndex(i)
                        .withAttempt(currentAttempt);

                queue.enqueueDelayed(deferred, backoff);

                log.info("Job {} rate-limited at step {}, deferring by {}s",
                        job.jobId(), i + 1, backoff.toSeconds());
                return;
            }

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

            logger.logMatch(job.uid(), automation, job.event(), result);

            boolean userVisibleSuccess =
                    result.messageSent() || isSaveContactSuccess(result, action);

            if (userVisibleSuccess) {
                if (!firedThisInvocation) {
                    cooldownService.recordFiring(job.uid(), automation, job.event());
                    firedThisInvocation = true;
                }

                currentAttempt = 1;
                continue;
            }

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

            deadLetterService.record(job.withActionIndex(i), automation,
                    "Non-retryable at step " + (i + 1) + ": " + result.error());
            return;
        }

        log.info("Job {} chain complete ({} action(s))", job.jobId(), actions.size());
    }

    private void runFollowGateAsk(AutomationJob job, Automation automation, InstagramAccount account) {
        String rateKey = account != null ? account.getInstagramUserId() : null;
        if (rateKey != null && !rateLimitService.tryAcquire(rateKey)) {
            Duration backoff = rateLimitService.suggestedBackoff(rateKey);
            if (backoff.isZero() || backoff.isNegative()) {
                backoff = Duration.ofSeconds(5);
            }
            queue.enqueueDelayed(job, backoff);
            log.info("Follow-gate ask rate-limited, deferring {}s", backoff.toSeconds());
            return;
        }

        ExecutionContext ctx = new ExecutionContext(job.uid(), automation, job.event(), account);
        ExecutionResult result;
        try {
            result = executor.executeFollowGateAsk(ctx);
        } catch (Exception ex) {
            log.error("Follow-gate ask threw for automation {}: {}",
                    automation.getId(), ex.getMessage(), ex);
            result = ExecutionResult.failed(null,
                    "Follow-gate ask exception: " + ex.getMessage(), null);
        }

        logger.logMatch(job.uid(), automation, job.event(), result);

        if (result.messageSent()) {
            cooldownService.recordFiring(job.uid(), automation, job.event());
            return;
        }

        if (retryPolicy.isRetryable(result)) {
            int nextAttempt = Math.max(1, job.attempt()) + 1;
            if (nextAttempt <= retryPolicy.maxAttempts()) {
                Duration delay = retryPolicy.backoffFor(nextAttempt);
                queue.enqueueDelayed(
                        job.withAttempt(nextAttempt).withLastError(result.error()), delay);
                log.info("Follow-gate ask failed retryably (attempt {}), retrying in {}s: {}",
                        nextAttempt, delay.toSeconds(), result.error());
                return;
            }
            deadLetterService.record(job, automation,
                    "Follow-gate ask max retries: " + result.error());
            return;
        }

        deadLetterService.record(job, automation,
                "Follow-gate ask failed: " + result.error());
    }

    private void runPublicReplyOnly(AutomationJob job, Automation automation, InstagramAccount account) {
        if (account == null) {
            log.warn("Public-reply-only: no connected IG account for job {}", job.jobId());
            return;
        }

        ExecutionContext ctx = new ExecutionContext(job.uid(), automation, job.event(), account);
        ExecutionResult result;
        try {
            result = executor.executePublicReplyOnly(ctx);
        } catch (Exception ex) {
            log.error("Public-reply-only threw for automation {}: {}",
                    automation.getId(), ex.getMessage(), ex);
            result = ExecutionResult.failed(null,
                    "Public-reply-only exception: " + ex.getMessage(), null);
        }

        logger.logMatch(job.uid(), automation, job.event(), result);

        if (result.messageSent()) {
            cooldownService.recordFiring(job.uid(), automation, job.event());
        } else {
            log.warn("Public-reply-only failed for automation {}: {}",
                    automation.getId(), result.error());
        }
    }

    private boolean isSaveContactSuccess(ExecutionResult result, Automation.Action action) {
        return result.error() == null && action.getType() == ActionType.SAVE_CONTACT;
    }
}