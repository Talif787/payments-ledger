package com.ledger.fakes;

import com.ledger.application.port.out.IdGenerator;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.transaction.TransactionId;
import java.util.UUID;

public final class RandomIdGenerator implements IdGenerator {
    @Override public AccountId newAccountId() { return new AccountId(UUID.randomUUID()); }
    @Override public TransactionId newTransactionId() { return new TransactionId(UUID.randomUUID()); }
}
