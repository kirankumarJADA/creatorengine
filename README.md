# CreatorEngine

End-to-end Instagram automation for creators, hardened for
production. A user comments "link" on a reel → CreatorEngine sees
the webhook, dedupes against redeliveries, checks the cooldown,
rate-limits, sends a Meta DM, and retries on transient failures.

> **Status:** auth, dashboard UI, automation builder, Instagram
> OAuth, webhook ingestion, automation execution engine, AND the
> production-hardening layer (queue, dedup, cooldown, retry,
> rate-limit, dead-letter, health) are all in place. The pipeline
> survives Meta redeliveries, transient 5xx/429 responses, and
> per-user spam bursts.

## Repository layout

```
creatorengine/
├── frontend/   React 18 + Vite + Tailwind + Zustand + Framer Motion
└── backend/    Spring Boot 3.3 + Spring Security + Firebase + Meta
```

## Production-hardened execution flow

```
  Meta sends POST /api/webhook        (may redeliver same event)
            │
            ▼
   WebhookController
   • verifies X-Hub-Signature-256 over RAW bytes
   • returns 200 fast
            │
            ▼
   WebhookService.processIncoming
   • parses payload → WebhookEventDto[]
   • finds owning user via collection-group lookup
   • saves the event record
   • calls engine.dispatch(uid, event)        ← sync, fast
            │
            ▼
   AutomationEngine.dispatch                  ← STAYS on webhook thread
   ├─ EventDeduplicationService.markIfNew     ← processed_events/{key}
   │     └─ duplicate? SKIP entire event
   ├─ AutomationMatcher.findCandidates        ← trigger + enabled filter
   ├─ ConditionEvaluator.evaluate             ← KEYWORD CONTAINS/EXACT
   ├─ CooldownService.canFire                 ← per (automation, sender)
   │     └─ cooling? silently drop
   └─ JobQueue.enqueue(AutomationJob)         ← one job per matched automation
            │
            ▼  (returns to Meta with 200 OK)
   QueueWorker (daemon thread pool)
   ├─ poll(1s) from JobQueue
   └─ engine.processJob(job)
            │
            ▼
   AutomationEngine.processJob                ← ON WORKER THREAD
   ├─ re-fetch automation         (toggle-off mid-flight respected)
   ├─ RateLimitService.tryAcquire  ← per IG account, msgs/min sliding window
   │     └─ over budget? requeueDelayed (NOT counted as retry)
   ├─ ActionExecutor.execute
   │  ├─ TemplateRenderer  ({{username}} → "aria.patel")
   │  ├─ MetaMessagingService.sendText   ← single attempt
   │  └─ ContactService.recordFromEvent
   ├─ ExecutionLogger.logMatch    ← success OR terminal failure
   └─ Outcome routing:
      ├─ success  → CooldownService.recordFiring
      ├─ retryable AND attempts < 3 → queue.enqueueDelayed(retry)
      └─ exhausted / non-retryable  → DeadLetterService.record
```

## Hardening layers — what each one does

| Layer | Code | Persists to | Purpose |
|-------|------|-------------|---------|
| **Dedup** | `automation/dedup/` | `processed_events/{dedupKey}` | Meta redelivers webhooks aggressively. First touch records a 7-day-TTL doc keyed on commentId or message mid; subsequent touches see it and skip the whole event. Set Firestore TTL policy on `expiresAt`. |
| **Queue** | `automation/queue/` | (in-memory) | `JobQueue` interface + `InMemoryJobQueue` impl. Two enqueue paths: immediate + delayed. `QueueWorker` is a daemon thread pool that polls and dispatches. Swap for Redis/SQS by replacing the impl. |
| **Cooldown** | `automation/cooldown/` | `users/{uid}/cooldowns/{automationId}:{senderIgId}` | Per-(automation, sender) anti-spam. Persistent because process restarts shouldn't re-open the floodgates. Cooldown miss = silent drop. |
| **Rate limit** | `automation/ratelimit/` | (in-memory) | Per-IG-account sliding-window counter, default 30 msgs/min, configurable via `app.rate-limit.messages-per-minute`. On hit, the job is requeued with backoff (NOT a retry). |
| **Retry** | `automation/retry/` | – | `RetryPolicy.isRetryable` uses `ExecutionResult.httpStatus`: 429/5xx/network → retry, 401/403/4xx → no. Backoffs 1s/3s/10s before attempts 2/3/(unused). Re-enqueue carries forward attempt count + last error. |
| **Dead letter** | `automation/deadletter/` | `users/{uid}/failed_jobs/{id}` | Terminal failures — either exhausted retries or non-retryable from the first attempt. Fields exactly match the spec: eventId, automationId, reason, attempts, jobId, createdAt. |
| **Health** | `health/` | – | `GET /api/health` (public) reports queue depth + worker liveness, time-since-last-webhook, Firestore probe latency, and Meta config presence. |

## Module layout (backend)

```
com.creatorengine
├── automation
│   ├── controller       AutomationController (REST CRUD)
│   ├── cooldown         CooldownService                       ← NEW
│   ├── deadletter       DeadLetterService, FailedJob*         ← NEW
│   ├── dedup            EventDeduplicationService, ProcessedEvent*  ← NEW
│   ├── dto              AutomationRequest/Response, …
│   ├── engine           AutomationEngine (dispatch + processJob)
│   ├── entity           Automation (+ cooldownMinutes field), ExecutionLog, enums
│   ├── executor         ActionExecutor, TemplateRenderer, …
│   ├── logger           ExecutionLogger
│   ├── matcher          AutomationMatcher, ConditionEvaluator
│   ├── queue            JobQueue, InMemoryJobQueue, QueueWorker, AutomationJob  ← NEW
│   ├── ratelimit        RateLimitService                      ← NEW
│   ├── repository       AutomationRepository, ExecutionLogRepository
│   ├── retry            RetryPolicy                           ← NEW
│   └── service          AutomationService
├── auth                 (register/login/refresh/forgot-password)
├── contacts             Contact, repository, service
├── health               HealthController, HealthService       ← NEW
├── instagram
│   ├── controller       InstagramController, WebhookController, WebhookTestController
│   ├── dto              + WebhookEventDto.dedupKey()
│   ├── entity           InstagramAccount, WebhookEventRecord (+ messageId)
│   ├── repository       …
│   └── service          + MetaMessagingService (single-attempt; retries belong to RetryPolicy)
└── common / config / exception / security
```

## API surface (final)

| Method | Path                          | Auth          | Purpose                              |
|--------|-------------------------------|---------------|--------------------------------------|
| GET/POST/PUT/DELETE | `/api/automations[/{id}]` | **JWT**       | Automation CRUD (incl. `cooldownMinutes`) |
| PATCH  | `/api/automations/{id}/toggle`| **JWT**       | Enable/disable                       |
| GET    | `/api/instagram/connect`      | **JWT**       | Returns the Meta OAuth URL           |
| GET    | `/api/instagram/callback`     | signed state  | Exchanges code for tokens            |
| POST   | `/api/instagram/disconnect`   | **JWT**       | Deletes stored account               |
| GET    | `/api/instagram/status`       | **JWT**       | Connection status + safe profile     |
| GET    | `/api/webhook`                | verify token  | Meta verification handshake          |
| POST   | `/api/webhook`                | X-Hub-Sig-256 | Receive events                       |
| POST   | `/api/webhook/test/simulate`  | **JWT** (dev) | Synthesize a single event            |
| POST   | `/api/webhook/test/raw`       | **JWT** (dev) | Replay a raw Meta payload            |
| **GET**| **`/api/health`**             | **none**      | **Queue + webhook + Firestore + Meta status** |

## Configuration

`backend/.env.example` gained:

```bash
# Queue worker pool size. Bound it to whatever your Meta rate limits + DB
# can sustain. 2 is enough for most accounts; raise on busier deployments.
APP_QUEUE_WORKERS=2

# Soft per-account rate limit. Below Meta's enforced limits so we never
# bump into them under normal traffic.
APP_RATE_LIMIT_MESSAGES_PER_MINUTE=30
```

`application.yml` reads these via:
```yaml
app:
  queue:
    workers: ${APP_QUEUE_WORKERS:2}
  rate-limit:
    messages-per-minute: ${APP_RATE_LIMIT_MESSAGES_PER_MINUTE:30}
```

## Required Firestore indexes

```
Collection group:  instagram_account
Field:             instagramUserId  (Ascending)
Scope:             Collection group

Each per-user subcollection benefits from a single-field index on its
timestamp ordering field (Firestore auto-creates these on first read):
  instagram_events       receivedAt  desc
  execution_logs         timestamp   desc
  contacts               updatedAt   desc
  failed_jobs            createdAt   desc

TTL policies to enable (Console → Firestore → TTL):
  processed_events       expiresAt
```

## What's still on the production checklist

- [ ] Swap `InMemoryJobQueue` for a Redis/SQS-backed implementation
      (the {@link com.creatorengine.automation.queue.JobQueue} interface
      is the seam).
- [ ] Wire a bounded queue + rejection policy on `InMemoryJobQueue` —
      currently unbounded.
- [ ] Encrypt `InstagramAccount.accessToken` at rest using a KMS-backed cipher.
- [ ] Wire the long-lived token refresh `@Scheduled` job
      (placeholder in `InstagramApiClient`).
- [ ] Surface a real Meta liveness probe (today health reports config
      presence only — see the "note" field in the response).
- [ ] Switch `WebhookService.processIncoming` to fail-closed
      (drop the dev-only "skip verification when secret is blank" branch).
- [ ] Disable the `dev` profile in prod so `WebhookTestController`
      doesn't register.

## What's intentionally not built

- Analytics / dashboards on top of execution_logs and failed_jobs.
- Broadcasts / batch DMs.
- AI / generative response composition.
- UI exposure of `cooldownMinutes` — set via API for now per spec
  ("No UI changes yet").

## License

Proprietary. All rights reserved.
