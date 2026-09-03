package com.ledger.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TransactionPosted(
        String transactionId,
        String currency,
        List<Entry> postings,
        Map<String, String> metadata,
        Instant occurredAt) implements DomainEvent {

    public record Entry(String accountId, String direction, long minorUnits) {}

    @Override public String eventType() { return "ledger.transaction.posted.v1"; }
    @Override public String aggregateId() { return transactionId; }
}
