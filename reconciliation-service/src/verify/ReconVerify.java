package com.reconciliation.verify;

import com.reconciliation.application.Reconciler;
import com.reconciliation.domain.AccountSnapshot;
import com.reconciliation.domain.DiscrepancyKind;
import com.reconciliation.domain.ReconciliationReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReconVerify {
    private static int passed = 0, failed = 0;
    private static final Instant NOW = Instant.parse("2026-01-15T00:00:00Z");

    public static void main(String[] args) {
        Reconciler reconciler = new Reconciler();

        // All three derivations agree and the book nets to zero.
        var healthy = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100),
                        new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L, "b", 100L), NOW);
        check("healthy ledger reconciles clean", healthy.isBalanced() && healthy.status().equals("OK"));
        check("healthy report counts both accounts", healthy.accountsChecked() == 2);

        // Stored balance disagrees with the sum of its postings.
        var corrupt = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -90, -100),
                        new AccountSnapshot("b", 100, 100)),
                Map.of("a", -90L, "b", 100L), NOW);
        check("balance/posting mismatch is detected",
                corrupt.discrepancies().stream().anyMatch(d -> d.kind() == DiscrepancyKind.BALANCE_POSTING_MISMATCH));
        // note: this corrupt ledger also fails global net (-90+100=10)
        check("global imbalance is detected",
                corrupt.discrepancies().stream().anyMatch(d -> d.kind() == DiscrepancyKind.GLOBAL_IMBALANCE));

        // Event-derived balance disagrees with the (self-consistent, zero-net) ledger.
        var drift = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100),
                        new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L, "b", 95L), NOW);
        check("event/ledger mismatch is detected",
                drift.discrepancies().stream().anyMatch(d -> d.kind() == DiscrepancyKind.EVENT_LEDGER_MISMATCH));
        check("drift status is DRIFT", drift.status().equals("DRIFT"));

        // Consumer lag: an account not yet seen in the stream is skipped, not flagged.
        var lag = reconciler.reconcile(
                List.of(new AccountSnapshot("a", -100, -100),
                        new AccountSnapshot("b", 100, 100)),
                Map.of("a", -100L), NOW);
        check("consumer lag is treated as lag, not drift", lag.isBalanced());

        System.out.println();
        System.out.println("RECON RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
}
