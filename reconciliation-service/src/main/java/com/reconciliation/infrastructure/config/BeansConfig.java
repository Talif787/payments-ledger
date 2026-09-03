package com.reconciliation.infrastructure.config;

import com.reconciliation.application.Reconciler;
import com.reconciliation.application.port.LedgerReadPort;
import com.reconciliation.application.port.ProjectionStore;
import com.reconciliation.application.service.ProjectionUpdater;
import com.reconciliation.application.service.ReconciliationRunner;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Reconciler reconciler() {
        return new Reconciler();
    }

    @Bean
    public ProjectionUpdater projectionUpdater(ProjectionStore store) {
        return new ProjectionUpdater(store);
    }

    @Bean
    public ReconciliationRunner reconciliationRunner(LedgerReadPort ledgerReadPort,
                                                     ProjectionStore projectionStore,
                                                     Reconciler reconciler,
                                                     Clock clock) {
        return new ReconciliationRunner(ledgerReadPort, projectionStore, reconciler, clock);
    }
}
