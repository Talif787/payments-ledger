package com.ledger.application.port.out;

import com.ledger.application.model.OutboxRecord;

/**
 * Publishes an outbox record to the event log as a versioned envelope, keyed for
 * per-aggregate ordering. Implementations publish synchronously and only return
 * normally once the log has acknowledged the write, so the relay can safely mark
 * the row published afterwards.
 */
public interface EventPublisher {
    void publish(String topic, String key, OutboxRecord record);
}
