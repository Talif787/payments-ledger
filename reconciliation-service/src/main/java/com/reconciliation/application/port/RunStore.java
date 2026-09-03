package com.reconciliation.application.port;

import com.reconciliation.domain.ReconciliationReport;

/** Persists a summary of each reconciliation pass for audit and trend analysis. */
public interface RunStore {
    void save(ReconciliationReport report);
}
