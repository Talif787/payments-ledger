package com.reconciliation.application.service;

import com.reconciliation.application.Reconciler;
import com.reconciliation.application.port.LedgerReadPort;
import com.reconciliation.application.port.ProjectionStore;
import com.reconciliation.domain.ReconciliationReport;
import java.time.Clock;

/**
 * Coordinates one reconciliation pass: load the ledger's authoritative snapshots
 * and the event-derived balances, then compare them with the pure reconciler.
 */
public final class ReconciliationRunner {

    private final LedgerReadPort ledgerReadPort;
    private final ProjectionStore projectionStore;
    private final Reconciler reconciler;
    private final Clock clock;

    public ReconciliationRunner(LedgerReadPort ledgerReadPort,
                                ProjectionStore projectionStore,
                                Reconciler reconciler,
                                Clock clock) {
        this.ledgerReadPort = ledgerReadPort;
        this.projectionStore = projectionStore;
        this.reconciler = reconciler;
        this.clock = clock;
    }

    public ReconciliationReport run() {
        return reconciler.reconcile(
                ledgerReadPort.loadAccountSnapshots(),
                projectionStore.derivedBalances(),
                clock.instant());
    }
}
