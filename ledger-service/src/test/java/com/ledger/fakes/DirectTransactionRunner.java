package com.ledger.fakes;

import com.ledger.application.port.out.TransactionRunner;
import java.util.function.Supplier;

/** Runs the unit of work directly, without a real database transaction. */
public final class DirectTransactionRunner implements TransactionRunner {
    @Override public <T> T inSerializableTransaction(Supplier<T> work) { return work.get(); }
}
