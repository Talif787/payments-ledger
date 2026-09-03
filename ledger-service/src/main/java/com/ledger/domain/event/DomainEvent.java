package com.ledger.domain.event;

import java.time.Instant;

/**
 * Marker for events the domain emits. Events are appended to the transactional
 * outbox in the same database transaction as the state change that produced
 * them, which is how the ledger achieves exactly-once publication downstream.
 */
public sealed interface DomainEvent permits AccountOpened, TransactionPosted {
    String eventType();
    String aggregateId();
    Instant occurredAt();
}
