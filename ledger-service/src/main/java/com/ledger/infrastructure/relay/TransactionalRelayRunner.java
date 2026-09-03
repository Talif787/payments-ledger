package com.ledger.infrastructure.relay;

import com.ledger.application.service.OutboxRelayService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one relay batch inside a database transaction. Kept as its own bean (not
 * a method on the scheduler) so the transactional proxy actually applies: claim,
 * publish, and mark-published all commit together, or roll back together if a
 * publish fails. Default isolation (read committed) is correct here because the
 * outbox claim relies on FOR UPDATE SKIP LOCKED, not on serializable isolation.
 */
@Component
@ConditionalOnProperty(name = "ledger.relay.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionalRelayRunner {

    private final OutboxRelayService relayService;

    public TransactionalRelayRunner(OutboxRelayService relayService) {
        this.relayService = relayService;
    }

    @Transactional
    public int runOnce(int batchSize) {
        return relayService.publishBatch(batchSize);
    }
}
