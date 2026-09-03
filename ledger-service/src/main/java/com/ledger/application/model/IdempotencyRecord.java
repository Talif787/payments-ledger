package com.ledger.application.model;

import com.ledger.domain.transaction.IdempotencyKey;
import com.ledger.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.Objects;

/**
 * Persistent record of a completed money-movement request, keyed by its
 * idempotency key. The request fingerprint lets the system distinguish a true
 * retry (same key, same request) from a conflicting reuse (same key, different
 * request).
 */
public record IdempotencyRecord(
        IdempotencyKey key,
        String requestFingerprint,
        TransactionId transactionId,
        String status,
        Instant createdAt) {

    public IdempotencyRecord {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
