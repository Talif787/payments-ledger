package com.ledger.fakes;

import com.ledger.application.model.OutboxRecord;
import com.ledger.application.port.out.EventPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FakeEventPublisher implements EventPublisher {
    public record Sent(String topic, String key, UUID eventId) {}
    public final List<Sent> sent = new ArrayList<>();
    private UUID failOn;

    public void failOnEvent(UUID eventId) { this.failOn = eventId; }

    @Override
    public void publish(String topic, String key, OutboxRecord record) {
        if (record.eventId().equals(failOn)) {
            throw new IllegalStateException("simulated publish failure");
        }
        sent.add(new Sent(topic, key, record.eventId()));
    }
}
