package com.creatorengine.automation.executor;

import lombok.Builder;

/**
 * What happened when we tried to run one automation against one event.
 *
 * <p>The engine collects these into execution logs — they're also
 * suitable for surfacing in a future "recent runs" UI without further
 * conversion.</p>
 */
@Builder
public record ExecutionResult(
        /** True iff an outbound message was actually sent. False for SAVE_CONTACT
         *  and for failed sends. */
        boolean messageSent,
        /** What we actually pushed to Meta (or for SAVE_CONTACT, the trigger text we recorded). */
        String renderedMessage,
        /** Meta's mid, when present. */
        String metaMessageId,
        /** Human-readable failure cause, or null on success. */
        String error,
        /**
         * HTTP status from Meta when applicable. 0 for network/unknown errors,
         * null for non-send failures (e.g. SAVE_CONTACT). Used by
         * {@link com.creatorengine.automation.retry.RetryPolicy} to decide
         * whether the failure is retryable.
         */
        Integer httpStatus
) {
    public static ExecutionResult sent(String message, String metaMessageId) {
        return ExecutionResult.builder()
                .messageSent(true)
                .renderedMessage(message)
                .metaMessageId(metaMessageId)
                .httpStatus(200)
                .build();
    }
    public static ExecutionResult savedOnly(String contextMessage) {
        return ExecutionResult.builder()
                .messageSent(false)
                .renderedMessage(contextMessage)
                .build();
    }
    public static ExecutionResult failed(String renderedMessage, String error, Integer httpStatus) {
        return ExecutionResult.builder()
                .messageSent(false)
                .renderedMessage(renderedMessage)
                .error(error)
                .httpStatus(httpStatus)
                .build();
    }
    /** Convenience for non-HTTP failures (config errors etc.). */
    public static ExecutionResult failed(String renderedMessage, String error) {
        return failed(renderedMessage, error, null);
    }
}
