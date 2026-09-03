package com.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.reconciliation.domain.AccountSnapshot;
import com.reconciliation.domain.DiscrepancyKind;
import com.reconciliation.domain.ReconciliationReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReconcilerTest {

    private final Reconciler reconciler = new Reconciler();
    private static final Instant NOW = Instant.parse("2026-01-15T00:00:00Z");

    @Test
    void reconcilesCleanWhenAllDerivationsAgree() {
        ReconciliationReport report = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100), new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L, "b", 100L), NOW);

        assertThat(report.isBalanced()).isTrue();
        assertThat(report.status()).isEqualTo("OK");
        assertThat(report.accountsChecked()).isEqualTo(2);
    }

    @Test
    void detectsBalancePostingMismatch() {
        ReconciliationReport report = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -90), new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L, "b", 100L), NOW);

        assertThat(report.discrepancies())
                .anyMatch(d -> d.kind() == DiscrepancyKind.BALANCE_POSTING_MISMATCH);
    }

    @Test
    void detectsEventLedgerMismatch() {
        ReconciliationReport report = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100), new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L, "b", 95L), NOW);

        assertThat(report.discrepancies())
                .anyMatch(d -> d.kind() == DiscrepancyKind.EVENT_LEDGER_MISMATCH);
        assertThat(report.status()).isEqualTo("DRIFT");
    }

    @Test
    void detectsGlobalImbalance() {
        ReconciliationReport report = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100), new AccountSnapshot("b", 90, 90)),
                Map.of("a", -100L, "b", 90L), NOW);

        assertThat(report.discrepancies())
                .anyMatch(d -> d.kind() == DiscrepancyKind.GLOBAL_IMBALANCE);
    }

    @Test
    void treatsConsumerLagAsLagNotDrift() {
        ReconciliationReport report = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100), new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L), NOW);

        assertThat(report.isBalanced()).isTrue();
    }
}
