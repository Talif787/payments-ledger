package com.reconciliation.application;

import com.reconciliation.domain.AccountSnapshot;
import com.reconciliation.domain.Discrepancy;
import com.reconciliation.domain.DiscrepancyKind;
import com.reconciliation.domain.ReconciliationReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure reconciliation logic. Given the ledger's authoritative account snapshots
 * and the balances independently re-derived from the event stream, it checks
 * three invariants and reports every violation:
 *
 *   1. Each stored balance equals the sum of that account's postings.
 *   2. Each event-derived balance equals the stored balance (for accounts the
 *      consumer has caught up on).
 *   3. All stored balances net to zero across the book.
 *
 * Accounts the event consumer has not yet caught up on are treated as lag, not
 * drift, and are skipped for check (2); an operator distinguishes lag from drift
 * by whether the difference persists across passes.
 */
public final class Reconciler {

    public ReconciliationReport reconcile(List<AccountSnapshot> ledger,
                                          Map<String, Long> eventDerivedBalances,
                                          Instant runAt) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        long globalNet = 0L;

        for (AccountSnapshot account : ledger) {
            globalNet += account.balanceMinor();

            if (account.balanceMinor() != account.postingSumMinor()) {
                discrepancies.add(new Discrepancy(
                        DiscrepancyKind.BALANCE_POSTING_MISMATCH,
                        account.accountId(),
                        account.postingSumMinor(),
                        account.balanceMinor()));
            }

            Long derived = eventDerivedBalances.get(account.accountId());
            if (derived != null && derived != account.balanceMinor()) {
                discrepancies.add(new Discrepancy(
                        DiscrepancyKind.EVENT_LEDGER_MISMATCH,
                        account.accountId(),
                        account.balanceMinor(),
                        derived));
            }
        }

        if (globalNet != 0L) {
            discrepancies.add(new Discrepancy(
                    DiscrepancyKind.GLOBAL_IMBALANCE, null, 0L, globalNet));
        }

        return new ReconciliationReport(runAt, ledger.size(), List.copyOf(discrepancies));
    }
}
