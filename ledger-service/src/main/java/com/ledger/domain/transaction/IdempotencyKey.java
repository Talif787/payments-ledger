package com.ledger.domain.transaction;

import com.ledger.domain.exception.DomainException;
import java.util.Objects;

/**
 * Client-supplied key that makes a money-movement request safe to retry. The
 * same key with the same request must yield the same result exactly once; the
 * same key with a different request is a conflict.
 */
public record IdempotencyKey(String value) {

    private static final int MAX_LENGTH = 255;

    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotency key");
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new DomainException.InvalidTransaction("Idempotency-Key must not be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new DomainException.InvalidTransaction("Idempotency-Key too long");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
