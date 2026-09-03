package com.ledger.application.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single unpublished (or in-flight) row read from the transactional outbox.
 * The relay turns each record into an event on the log. {@code eventId} is the
 * stable, unique identity a consumer uses to deduplicate, since delivery is
 * at-least-once.
 */
public record OutboxRecord(
        long id,
        UUID eventId,
        String eventType,
        String aggregateId,
        String payloadJson,
        Instant occurredAt) {

    public OutboxRecord {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
