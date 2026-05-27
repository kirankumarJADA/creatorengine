package com.creatorengine.automation.executor;

public record ExecutionResult(
        boolean messageSent,
        String renderedMessage,
        String metaMessageId,
        String error,
        Integer httpStatus
) {
    public static ExecutionResult sent(String message, String metaMessageId) {
        return new ExecutionResult(true, message, metaMessageId, null, 200);
    }

    public static ExecutionResult savedOnly(String contextMessage) {
        return new ExecutionResult(false, contextMessage, null, null, null);
    }

    public static ExecutionResult failed(String renderedMessage, String error, Integer httpStatus) {
        return new ExecutionResult(false, renderedMessage, null, error, httpStatus);
    }

    public static ExecutionResult failed(String renderedMessage, String error) {
        return failed(renderedMessage, error, null);
    }
}