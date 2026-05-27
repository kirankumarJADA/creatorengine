package com.creatorengine.automation.logger;

import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.entity.ExecutionLog;
import com.creatorengine.automation.executor.ExecutionResult;
import com.creatorengine.automation.repository.ExecutionLogRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ExecutionLogger {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLogger.class);

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COOLDOWN_SKIPPED = "COOLDOWN_SKIPPED";
    public static final String STATUS_DUPLICATE_IGNORED = "DUPLICATE_IGNORED";

    private final ExecutionLogRepository repository;

    public ExecutionLogger(ExecutionLogRepository repository) {
        this.repository = repository;
    }

    public void logMatch(
            String uid,
            Automation automation,
            WebhookEventDto event,
            ExecutionResult result
    ) {
        String status = deriveMatchStatus(result);

        ExecutionLog row = ExecutionLog.builder()
                .automationId(automation.getId())
                .automationName(automation.getName())
                .matched(true)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(result.messageSent())
                .status(status)
                .renderedMessage(result.renderedMessage())
                .actionType(automation.getAction() != null && automation.getAction().getType() != null
                        ? automation.getAction().getType().name()
                        : null)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .metaMessageId(result.metaMessageId())
                .errorMessage(result.error())
                .timestamp(Instant.now())
                .build();

        safeSave(uid, row, "match");
    }

    public void logCooldownSkipped(String uid, Automation automation, WebhookEventDto event) {
        ExecutionLog row = ExecutionLog.builder()
                .automationId(automation.getId())
                .automationName(automation.getName())
                .matched(true)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(false)
                .status(STATUS_COOLDOWN_SKIPPED)
                .actionType(automation.getAction() != null && automation.getAction().getType() != null
                        ? automation.getAction().getType().name()
                        : null)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .timestamp(Instant.now())
                .build();

        safeSave(uid, row, "cooldown-skip");
    }

    public void logDuplicateIgnored(String uid, WebhookEventDto event) {
        ExecutionLog row = ExecutionLog.builder()
                .matched(false)
                .eventType(event != null && event.type() != null ? event.type().name() : null)
                .triggerText(event != null ? event.message() : null)
                .messageSent(false)
                .status(STATUS_DUPLICATE_IGNORED)
                .recipientUsername(event != null ? event.username() : null)
                .recipientInstagramId(event != null ? event.instagramUserId() : null)
                .timestamp(Instant.now())
                .build();

        safeSave(uid, row, "dup-ignored");
    }

    private void safeSave(String uid, ExecutionLog row, String tag) {
        try {
            repository.save(uid, row);
            log.debug("Logged [{}] uid={} automation={} status={}",
                    tag, uid, row.getAutomationId(), row.getStatus());
        } catch (Exception ex) {
            log.warn("Failed to persist [{}] log uid={}: {}", tag, uid, ex.getMessage());
        }
    }

    private static String deriveMatchStatus(ExecutionResult result) {
        if (result == null) {
            return STATUS_FAILED;
        }

        if (result.messageSent()) {
            return STATUS_SUCCESS;
        }

        if (result.error() == null) {
            return STATUS_SUCCESS;
        }

        return STATUS_FAILED;
    }
}