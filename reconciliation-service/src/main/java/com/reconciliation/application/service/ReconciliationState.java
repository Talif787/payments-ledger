package com.reconciliation.application.service;

import com.reconciliation.domain.ReconciliationReport;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Holds the most recent reconciliation report for the API and metrics to read. */
@Component
public class ReconciliationState {

    private final AtomicReference<ReconciliationReport> latest = new AtomicReference<>();

    public void update(ReconciliationReport report) {
        latest.set(report);
    }

    public Optional<ReconciliationReport> latest() {
        return Optional.ofNullable(latest.get());
    }

    public int latestDiscrepancyCount() {
        ReconciliationReport report = latest.get();
        return report == null ? 0 : report.discrepancies().size();
    }
}
