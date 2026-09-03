package com.ledger.application.port.out;

import java.util.function.Supplier;

/**
 * Executes a unit of work inside a single serializable database transaction,
 * transparently retrying on serialization failures. This is the seam that lets
 * the application layer express "do all of this atomically and correctly under
 * concurrency" without depending on any persistence framework.
 */
public interface TransactionRunner {
    <T> T inSerializableTransaction(Supplier<T> work);
}
