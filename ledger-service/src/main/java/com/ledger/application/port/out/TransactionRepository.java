package com.ledger.application.port.out;

import com.ledger.domain.transaction.LedgerTransaction;
import com.ledger.domain.transaction.TransactionId;
import java.util.Optional;

public interface TransactionRepository {
    void persist(LedgerTransaction transaction);
    Optional<LedgerTransaction> findById(TransactionId id);
}
