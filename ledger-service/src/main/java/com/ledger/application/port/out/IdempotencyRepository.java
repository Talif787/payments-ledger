package com.ledger.application.port.out;

import com.ledger.application.model.IdempotencyRecord;
import com.ledger.domain.transaction.IdempotencyKey;
import java.util.Optional;

public interface IdempotencyRepository {
    Optional<IdempotencyRecord> find(IdempotencyKey key);
    void save(IdempotencyRecord record);
}
