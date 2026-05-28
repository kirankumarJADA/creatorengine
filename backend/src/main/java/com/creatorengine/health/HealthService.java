package com.creatorengine.health;

import com.creatorengine.automation.queue.QueueWorker;
import com.creatorengine.automation.ratelimit.RateLimitService;
import com.creatorengine.config.AppProperties;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final QueueWorker queueWorker;
    private final RateLimitService rateLimitService;
    private final Firestore firestore;
    private final AppProperties appProperties;

    private final AtomicLong lastWebhookEpochMs = new AtomicLong(0);

    public HealthService(
            QueueWorker queueWorker,
            RateLimitService rateLimitService,
            Firestore firestore,
            AppProperties appProperties
    ) {
        this.queueWorker = queueWorker;
        this.rateLimitService = rateLimitService;
        this.firestore = firestore;
        this.appProperties = appProperties;
    }

    public void markWebhookHit() {
        lastWebhookEpochMs.set(System.currentTimeMillis());
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("status", "ok");
        out.put("timestamp", Instant.now().toString());
        out.put("queue", queueSnapshot());
        out.put("webhook", webhookSnapshot());
        out.put("firestore", firestoreSnapshot());
        out.put("meta", metaSnapshot());
        out.put("rateLimit", rateLimitSnapshot());

        boolean allOk = subsystemHealthy(out.get("queue"))
                && subsystemHealthy(out.get("firestore"))
                && subsystemHealthy(out.get("meta"));

        out.put("status", allOk ? "ok" : "degraded");

        return out;
    }

    private Map<String, Object> queueSnapshot() {
        var s = queueWorker.status();
        boolean healthy = s.alive() == s.configured();

        return Map.of(
                "healthy", healthy,
                "workersAlive", s.alive(),
                "workersConfigured", s.configured(),
                "queueDepth", s.queueDepth(),
                "processedTotal", s.processedTotal()
        );
    }

    private Map<String, Object> webhookSnapshot() {
        long ts = lastWebhookEpochMs.get();

        if (ts == 0) {
            return Map.of(
                    "healthy", true,
                    "lastReceivedAt", "never",
                    "secondsSinceLast", -1
            );
        }

        long secs = (System.currentTimeMillis() - ts) / 1000L;

        return Map.of(
                "healthy", true,
                "lastReceivedAt", Instant.ofEpochMilli(ts).toString(),
                "secondsSinceLast", secs
        );
    }

    private Map<String, Object> firestoreSnapshot() {
        long started = System.nanoTime();
        boolean healthy;
        String error = null;

        try {
            firestore.listCollections().iterator().hasNext();
            healthy = true;
        } catch (Exception ex) {
            healthy = false;
            error = ex.getMessage();
            log.warn("Firestore health probe failed: {}", ex.getMessage());
        }

        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("healthy", healthy);
        out.put("probeMs", ms);

        if (error != null) {
            out.put("error", error);
        }

        return out;
    }

    private Map<String, Object> metaSnapshot() {
        AppProperties.Meta m = appProperties.getMeta();

        boolean configured = m != null
                && m.getAppId() != null && !m.getAppId().isBlank()
                && m.getAppSecret() != null && !m.getAppSecret().isBlank();

        return Map.of(
                "healthy", configured,
                "configured", configured,
                "graphApiVersion", m != null ? m.getGraphApiVersion() : "?",
                "note", "Liveness ping not implemented - reporting config presence only."
        );
    }

    private Map<String, Object> rateLimitSnapshot() {
        return Map.of(
                "limitPerMinute", rateLimitService.limitPerMinute(),
                "trackedAccounts", rateLimitService.trackedAccounts()
        );
    }

    private boolean subsystemHealthy(Object subsystem) {
        if (!(subsystem instanceof Map<?, ?> m)) {
            return true;
        }

        Object h = m.get("healthy");
        return !(h instanceof Boolean b) || b;
    }
}