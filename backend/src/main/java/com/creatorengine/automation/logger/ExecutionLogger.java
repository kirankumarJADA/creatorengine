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

    private final ExecutionLogRepository repository;

    public ExecutionLogger(ExecutionLogRepository repository) {
        this.repository = repository;
    }

    public void logMatch(String uid, Automation automation, WebhookEventDto event, ExecutionResult result) {
        if (uid == null) return;
        String igAccountId = event != null ? event.receivingAccountId() : null;

        ExecutionLog row = new ExecutionLog();
        row.setTimestamp(Instant.now());
        row.setAutomationId(automation != null ? automation.getId() : null);
        row.setAutomationName(automation != null ? automation.getName() : null);
        if (automation != null && automation.getTrigger() != null) {
            row.setEventType(automation.getTrigger().name());
        }
        row.setRecipientUsername(event != null ? event.username() : null);
        row.setRecipientInstagramId(event != null ? event.instagramUserId() : null);
        row.setTriggerText(event != null ? event.message() : null);
        row.setMessageSent(result != null && result.messageSent());
        row.setMatched(true);
        row.setStatus(deriveStatus(result));
        row.setErrorMessage(result != null ? result.error() : null);

        try {
            repository.save(uid, igAccountId, row);
        } catch (Exception ex) {
            log.warn("ExecutionLogger.logMatch failed uid={}: {}", uid, ex.getMessage());
        }
    }

    public void logDuplicateIgnored(String uid, WebhookEventDto event) {
        if (uid == null) return;
        String igAccountId = event != null ? event.receivingAccountId() : null;

        ExecutionLog row = new ExecutionLog();
        row.setTimestamp(Instant.now());
        if (event != null && event.type() != null) {
            row.setEventType(event.type().name());
        }
        row.setRecipientUsername(event != null ? event.username() : null);
        row.setRecipientInstagramId(event != null ? event.instagramUserId() : null);
        row.setTriggerText(event != null ? event.message() : null);
        row.setMessageSent(false);
        row.setMatched(false);
        row.setStatus("DUPLICATE_IGNORED");

        try {
            repository.save(uid, igAccountId, row);
        } catch (Exception ex) {
            log.warn("ExecutionLogger.logDuplicateIgnored failed uid={}: {}", uid, ex.getMessage());
        }
    }

    public void logCooldownSkipped(String uid, Automation automation, WebhookEventDto event) {
        if (uid == null) return;
        String igAccountId = event != null ? event.receivingAccountId() : null;

        ExecutionLog row = new ExecutionLog();
        row.setTimestamp(Instant.now());
        row.setAutomationId(automation != null ? automation.getId() : null);
        row.setAutomationName(automation != null ? automation.getName() : null);
        if (automation != null && automation.getTrigger() != null) {
            row.setEventType(automation.getTrigger().name());
        }
        row.setRecipientUsername(event != null ? event.username() : null);
        row.setRecipientInstagramId(event != null ? event.instagramUserId() : null);
        row.setTriggerText(event != null ? event.message() : null);
        row.setMessageSent(false);
        row.setMatched(true);
        row.setStatus("COOLDOWN_SKIPPED");

        try {
            repository.save(uid, igAccountId, row);
        } catch (Exception ex) {
            log.warn("ExecutionLogger.logCooldownSkipped failed uid={}: {}", uid, ex.getMessage());
        }
    }

    private static String deriveStatus(ExecutionResult result) {
        if (result == null) return "FAILED";
        if (result.messageSent()) return "SUCCESS";
        if (result.error() != null) return "FAILED";
        return "SUCCESS";
    }
}