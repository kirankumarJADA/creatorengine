package com.creatorengine.automation.entity;

/** What happens when an automation fires. */
public enum ActionType {
    SEND_DM,
    SEND_MESSAGE,
    SEND_LINK,
    SAVE_CONTACT,
    /**
     * Pause the chain before continuing. The engine handles this
     * specially — it does NOT block the worker thread; instead it
     * enqueues a continuation job via {@code JobQueue.enqueueDelayed}
     * and returns.
     */
    DELAY
}
