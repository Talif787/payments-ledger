package com.ledger.fakes;

import com.ledger.application.port.out.OutboxRepository;
import com.ledger.domain.event.DomainEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryOutboxRepository implements OutboxRepository {
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @Override public void append(DomainEvent event) { events.add(event); }
    public List<DomainEvent> events() { return List.copyOf(events); }
}
