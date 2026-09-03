package com.reconciliation.presentation.web;

import com.reconciliation.application.port.RunStore;
import com.reconciliation.application.service.ReconciliationRunner;
import com.reconciliation.application.service.ReconciliationState;
import com.reconciliation.domain.ReconciliationReport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the latest reconciliation result and an on-demand trigger. Handy for
 * demos and for an operator who wants to force a pass after an incident.
 */
@RestController
@RequestMapping("/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationRunner runner;
    private final ReconciliationState state;
    private final RunStore runStore;

    public ReconciliationController(ReconciliationRunner runner,
                                    ReconciliationState state,
                                    RunStore runStore) {
        this.runner = runner;
        this.state = state;
        this.runStore = runStore;
    }

    @GetMapping("/latest")
    public ResponseEntity<ReconciliationReport> latest() {
        return state.latest().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/run")
    public ReconciliationReport runNow() {
        ReconciliationReport report = runner.run();
        state.update(report);
        runStore.save(report);
        return report;
    }
}
