package com.ledger.fakes;

import com.ledger.application.model.IdempotencyRecord;
import com.ledger.application.port.out.IdempotencyRepository;
import com.ledger.domain.transaction.IdempotencyKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyRepository implements IdempotencyRepository {
    private final Map<IdempotencyKey, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @Override public Optional<IdempotencyRecord> find(IdempotencyKey key) { return Optional.ofNullable(store.get(key)); }
    @Override public void save(IdempotencyRecord record) { store.putIfAbsent(record.key(), record); }
}
