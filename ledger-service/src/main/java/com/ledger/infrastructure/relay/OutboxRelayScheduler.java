package com.ledger.infrastructure.relay;

import com.ledger.application.port.out.OutboxReader;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the relay on a fixed delay and exposes the outbox backlog as a gauge.
 * Failures are logged and swallowed so a transient broker problem does not kill
 * the scheduler thread; the unpublished rows are simply retried on the next tick.
 */
@Component
@ConditionalOnProperty(name = "ledger.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final TransactionalRelayRunner runner;
    private final OutboxReader reader;
    private final MeterRegistry meterRegistry;
    private final int batchSize;

    public OutboxRelayScheduler(TransactionalRelayRunner runner,
                                OutboxReader reader,
                                MeterRegistry meterRegistry,
                                @Value("${ledger.relay.batch-size:100}") int batchSize) {
        this.runner = runner;
        this.reader = reader;
        this.meterRegistry = meterRegistry;
        this.batchSize = batchSize;
    }

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("ledger.outbox.backlog", reader, r -> (double) r.backlogSize());
    }

    @Scheduled(fixedDelayString = "${ledger.relay.interval-ms:1000}")
    public void poll() {
        try {
            int published = runner.runOnce(batchSize);
            if (published > 0) {
                meterRegistry.counter("ledger.outbox.published").increment(published);
            }
        } catch (Exception e) {
            log.warn("Relay batch failed; will retry on next tick: {}", e.getMessage());
        }
    }
}
