package com.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.application.model.OutboxRecord;
import com.ledger.fakes.FakeEventPublisher;
import com.ledger.fakes.FakeOutboxReader;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxRelayServiceTest {

    private final TopicResolver topics = new TopicResolver("ledger.accounts.v1", "ledger.transactions.v1");

    private OutboxRecord record(long id, String type, String aggregate) {
        return new OutboxRecord(id, UUID.randomUUID(), type, aggregate, "{}", Instant.now());
    }

    @Test
    void publishesAndMarksEachRecord() {
        var reader = new FakeOutboxReader();
        var publisher = new FakeEventPublisher();
        reader.seed(record(1, "account.opened.v1", "acc-1"),
                    record(2, "ledger.transaction.posted.v1", "txn-1"));
        var relay = new OutboxRelayService(reader, publisher, topics);

        int published = relay.publishBatch(10);

        assertThat(published).isEqualTo(2);
        assertThat(publisher.sent).hasSize(2);
        assertThat(publisher.sent.get(0).topic()).isEqualTo("ledger.accounts.v1");
        assertThat(publisher.sent.get(1).topic()).isEqualTo("ledger.transactions.v1");
        assertThat(reader.marked).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void keysEventsByAggregateId() {
        var reader = new FakeOutboxReader();
        var publisher = new FakeEventPublisher();
        reader.seed(record(1, "ledger.transaction.posted.v1", "txn-42"));
        var relay = new OutboxRelayService(reader, publisher, topics);

        relay.publishBatch(10);

        assertThat(publisher.sent.get(0).key()).isEqualTo("txn-42");
    }

    @Test
    void doesNotMarkRecordWhosePublishFailed() {
        var reader = new FakeOutboxReader();
        var publisher = new FakeEventPublisher();
        var failing = record(11, "ledger.transaction.posted.v1", "txn-2");
        reader.seed(record(10, "account.opened.v1", "acc-2"), failing);
        publisher.failOnEvent(failing.eventId());
        var relay = new OutboxRelayService(reader, publisher, topics);

        assertThatThrownBy(() -> relay.publishBatch(10)).isInstanceOf(RuntimeException.class);
        assertThat(reader.marked).doesNotContain(11L);
    }
}
