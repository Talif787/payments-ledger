package com.ledger.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.application.port.out.OutboxRepository;
import com.ledger.domain.event.DomainEvent;
import java.sql.Timestamp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Appends domain events to the outbox table within the caller's transaction.
 * A separate relay (Phase 2) publishes unpublished rows to the event log and
 * marks them published, giving exactly-once delivery without dual writes.
 */
@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcOutboxRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(DomainEvent event) {
        String sql = """
                INSERT INTO outbox (aggregate_id, event_type, payload, created_at)
                VALUES (:aggregateId, :eventType, CAST(:payload AS jsonb), :createdAt)
                """;
        var params = new MapSqlParameterSource()
                .addValue("aggregateId", event.aggregateId())
                .addValue("eventType", event.eventType())
                .addValue("payload", toJson(event))
                .addValue("createdAt", Timestamp.from(event.occurredAt()));
        jdbc.update(sql, params);
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event", e);
        }
    }
}
