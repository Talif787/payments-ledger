package com.ledger.infrastructure.id;

import com.ledger.application.port.out.IdGenerator;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.transaction.TransactionId;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates identifiers as random (v4) UUIDs. A future optimization is to adopt
 * time-ordered UUIDv7 values to improve primary-key index locality under high
 * insert rates.
 */
@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public AccountId newAccountId() {
        return new AccountId(UUID.randomUUID());
    }

    @Override
    public TransactionId newTransactionId() {
        return new TransactionId(UUID.randomUUID());
    }
}
