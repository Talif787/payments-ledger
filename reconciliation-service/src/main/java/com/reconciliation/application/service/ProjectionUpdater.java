package com.reconciliation.application.service;

import com.reconciliation.application.port.ProjectionStore;
import java.util.List;
import java.util.UUID;

/**
 * Applies a transaction event to the derived-balance projection with
 * exactly-once effect. Deduplication is by event id: an event already consumed
 * is ignored, so the at-least-once delivery of the relay produces at-most-once
 * effects here. Intended to run inside a transaction so dedup and application
 * commit together.
 */
public final class ProjectionUpdater {

    private final ProjectionStore store;

    public ProjectionUpdater(ProjectionStore store) {
        this.store = store;
    }

    /** Returns true if the event was applied, false if it was a duplicate. */
    public boolean apply(UUID eventId, List<PostingDelta> deltas) {
        if (!store.markConsumed(eventId)) {
            return false;
        }
        for (PostingDelta delta : deltas) {
            store.applyDelta(delta.accountId(), delta.deltaMinor());
        }
        return true;
    }
}
