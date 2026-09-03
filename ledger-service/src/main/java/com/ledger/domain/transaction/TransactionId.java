package com.ledger.domain.transaction;

import java.util.Objects;
import java.util.UUID;

/** Server-generated identifier for a posted ledger transaction. */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "transaction id");
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
