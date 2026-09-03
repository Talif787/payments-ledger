package com.reconciliation.domain;

import java.time.Instant;
import java.util.List;

/**
 * The outcome of one reconciliation pass. A report with no discrepancies means
 * the ledger's stored balances, the sum of its postings, and the balances
 * re-derived from the event stream all agree, and the whole book nets to zero.
 */
public record ReconciliationReport(
        Instant runAt,
        int accountsChecked,
        List<Discrepancy> discrepancies) {

    public boolean isBalanced() {
        return discrepancies.isEmpty();
    }

    public String status() {
        return isBalanced() ? "OK" : "DRIFT";
    }
}
