package com.ledger.domain.event;

import java.time.Instant;

public record AccountOpened(
        String accountId,
        String currency,
        boolean allowOverdraft,
        Instant occurredAt) implements DomainEvent {

    @Override public String eventType() { return "account.opened.v1"; }
    @Override public String aggregateId() { return accountId; }
}
