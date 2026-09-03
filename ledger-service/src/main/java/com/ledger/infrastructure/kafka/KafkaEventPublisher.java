package com.ledger.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ledger.application.model.OutboxRecord;
import com.ledger.application.port.out.EventPublisher;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes an outbox record as a versioned JSON envelope, keyed by aggregate id
 * for per-aggregate ordering. The send is synchronous: the method only returns
 * once the broker acknowledges, and throws otherwise, so the relay's transaction
 * rolls back and the row is retried rather than being marked published.
 */
@Component
@ConditionalOnProperty(name = "ledger.relay.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher implements EventPublisher {

    private static final int SCHEMA_VERSION = 1;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, OutboxRecord record) {
        try {
            String envelope = buildEnvelope(record);
            kafkaTemplate.send(topic, key, envelope).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event " + record.eventId(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish event " + record.eventId(), e);
        }
    }

    private String buildEnvelope(OutboxRecord record) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", record.eventId().toString());
        envelope.put("eventType", record.eventType());
        envelope.put("schemaVersion", SCHEMA_VERSION);
        envelope.put("aggregateId", record.aggregateId());
        envelope.put("occurredAt", record.occurredAt().toString());
        envelope.set("data", objectMapper.readTree(record.payloadJson()));
        return objectMapper.writeValueAsString(envelope);
    }
}
