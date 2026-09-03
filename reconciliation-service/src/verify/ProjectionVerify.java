package com.reconciliation.verify;

import com.reconciliation.application.service.PostingDelta;
import com.reconciliation.application.service.ProjectionUpdater;
import com.reconciliation.fakes.InMemoryProjectionStore;
import java.util.List;
import java.util.UUID;

public final class ProjectionVerify {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        var store = new InMemoryProjectionStore();
        var updater = new ProjectionUpdater(store);
        UUID e1 = UUID.randomUUID();

        boolean firstApply = updater.apply(e1, List.of(
                new PostingDelta("a", -1000), new PostingDelta("b", 1000)));
        check("first delivery is applied", firstApply);
        check("balances reflect the event",
                store.derivedBalances().get("a") == -1000 && store.derivedBalances().get("b") == 1000);

        boolean secondApply = updater.apply(e1, List.of(
                new PostingDelta("a", -1000), new PostingDelta("b", 1000)));
        check("duplicate delivery is ignored", !secondApply);
        check("duplicate did not double-apply",
                store.derivedBalances().get("a") == -1000 && store.derivedBalances().get("b") == 1000);

        UUID e2 = UUID.randomUUID();
        updater.apply(e2, List.of(new PostingDelta("a", -500), new PostingDelta("b", 500)));
        check("distinct event applies on top",
                store.derivedBalances().get("a") == -1500 && store.derivedBalances().get("b") == 1500);

        System.out.println();
        System.out.println("PROJECTION RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
}
