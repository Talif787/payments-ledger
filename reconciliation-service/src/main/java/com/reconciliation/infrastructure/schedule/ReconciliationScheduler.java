package com.reconciliation.infrastructure.schedule;

import com.reconciliation.application.port.RunStore;
import com.reconciliation.application.service.ReconciliationRunner;
import com.reconciliation.application.service.ReconciliationState;
import com.reconciliation.domain.ReconciliationReport;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs reconciliation on a fixed delay, publishes the latest discrepancy count as
 * a gauge, and records each pass. A nonzero gauge is the signal an operator or
 * alert watches: it means the ledger, its postings, and the event stream have
 * stopped agreeing.
 */
@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final ReconciliationRunner runner;
    private final ReconciliationState state;
    private final RunStore runStore;
    private final MeterRegistry meterRegistry;

    public ReconciliationScheduler(ReconciliationRunner runner,
                                   ReconciliationState state,
                                   RunStore runStore,
                                   MeterRegistry meterRegistry) {
        this.runner = runner;
        this.state = state;
        this.runStore = runStore;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("reconciliation.discrepancies", state, ReconciliationState::latestDiscrepancyCount);
    }

    @Scheduled(fixedDelayString = "${recon.schedule-ms:15000}")
    public void reconcile() {
        try {
            ReconciliationReport report = runner.run();
            state.update(report);
            runStore.save(report);
            if (!report.isBalanced()) {
                log.warn("Reconciliation DRIFT: {} discrepancy(ies) across {} account(s)",
                        report.discrepancies().size(), report.accountsChecked());
            } else {
                log.info("Reconciliation OK across {} account(s)", report.accountsChecked());
            }
        } catch (Exception e) {
            log.error("Reconciliation pass failed: {}", e.getMessage());
        }
    }
}
