package com.reconciliation.domain;

/**
 * A single reconciliation failure. Amounts are signed integer minor units so the
 * report is exact. {@code accountId} is null for a system-wide discrepancy such
 * as global imbalance.
 */
public record Discrepancy(DiscrepancyKind kind, String accountId, long expectedMinor, long actualMinor) {

    public long differenceMinor() {
        return actualMinor - expectedMinor;
    }
}
