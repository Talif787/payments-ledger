package com.ledger.application.service;

import com.ledger.application.model.OutboxRecord;
import com.ledger.application.port.out.EventPublisher;
import com.ledger.application.port.out.OutboxReader;
import java.util.List;

/**
 * Publishes committed outbox rows to the event log.
 *
 * Each batch is intended to run inside one database transaction: rows are claimed
 * under a lock, published to the log in order, then marked published. If a
 * publish fails, the batch stops and the transaction rolls back, so the unclaimed
 * rows are retried on the next pass and nothing is marked published that was not
 * acknowledged. Because a crash can occur after the log acknowledges but before
 * the row is marked, delivery is at-least-once and consumers deduplicate by
 * eventId.
 */
public final class OutboxRelayService {

    private final OutboxReader reader;
    private final EventPublisher publisher;
    private final TopicResolver topicResolver;

    public OutboxRelayService(OutboxReader reader, EventPublisher publisher, TopicResolver topicResolver) {
        this.reader = reader;
        this.publisher = publisher;
        this.topicResolver = topicResolver;
    }

    /**
     * Publishes up to {@code batchSize} pending events. Returns the number
     * published. Runs within the caller's transaction.
     */
    public int publishBatch(int batchSize) {
        List<OutboxRecord> batch = reader.claimUnpublished(batchSize);
        int published = 0;
        for (OutboxRecord record : batch) {
            String topic = topicResolver.resolve(record.eventType());
            publisher.publish(topic, record.aggregateId(), record);
            reader.markPublished(record.id());
            published++;
        }
        return published;
    }
}
