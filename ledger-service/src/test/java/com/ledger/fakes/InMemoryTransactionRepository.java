package com.ledger.fakes;

import com.ledger.application.port.out.TransactionRepository;
import com.ledger.domain.transaction.LedgerTransaction;
import com.ledger.domain.transaction.TransactionId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<TransactionId, LedgerTransaction> store = new ConcurrentHashMap<>();

    @Override public void persist(LedgerTransaction tx) { store.put(tx.id(), tx); }
    @Override public Optional<LedgerTransaction> findById(TransactionId id) { return Optional.ofNullable(store.get(id)); }
}
