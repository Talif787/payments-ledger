package com.ledger.application.port.out;

import com.ledger.domain.event.DomainEvent;

/**
 * Appends a domain event to the transactional outbox. Implementations must
 * write within the caller's active database transaction so the event and the
 * state change commit atomically.
 */
public interface OutboxRepository {
    void append(DomainEvent event);
}
