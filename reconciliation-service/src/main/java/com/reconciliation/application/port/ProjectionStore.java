package com.reconciliation.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * The reconciliation service's own store for balances re-derived from the event
 * stream, plus consumer-side deduplication. Writes happen inside a single
 * transaction so marking an event consumed and applying its effects are atomic.
 */
public interface ProjectionStore {

    /** Records an event as consumed. Returns true if newly recorded, false if already seen. */
    boolean markConsumed(UUID eventId);

    /** Applies a signed minor-unit delta to an account's derived balance. */
    void applyDelta(String accountId, long deltaMinor);

    /** All derived balances, keyed by account id. */
    Map<String, Long> derivedBalances();
}
