package com.ledger.infrastructure.persistence.jdbc;

import com.ledger.application.model.OutboxRecord;
import com.ledger.application.port.out.OutboxReader;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the outbox for the relay. Rows are claimed with FOR UPDATE SKIP LOCKED so
 * multiple relay workers never publish the same event, and are returned in id
 * order for FIFO publication. Must be called inside a transaction; the lock is
 * held until that transaction commits (after the events are marked published).
 */
@Repository
public class JdbcOutboxReader implements OutboxReader {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcOutboxReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<OutboxRecord> claimUnpublished(int batchSize) {
        String sql = """
                SELECT id, event_id, event_type, aggregate_id, payload::text AS payload_json, created_at
                FROM outbox
                WHERE published = FALSE
                ORDER BY id
                FOR UPDATE SKIP LOCKED
                LIMIT :batchSize
                """;
        return jdbc.query(sql, new MapSqlParameterSource("batchSize", batchSize), (rs, rowNum) ->
                new OutboxRecord(
                        rs.getLong("id"),
                        rs.getObject("event_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("payload_json"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    public void markPublished(long id) {
        jdbc.update("UPDATE outbox SET published = TRUE, published_at = now() WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public void recordFailure(long id, String error) {
        String sql = """
                UPDATE outbox
                SET attempts = attempts + 1, last_attempt_at = now(), last_error = :error
                WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource().addValue("id", id).addValue("error", error));
    }

    @Override
    public long backlogSize() {
        Long count = jdbc.getJdbcTemplate().queryForObject(
                "SELECT count(*) FROM outbox WHERE published = FALSE", Long.class);
        return count == null ? 0L : count;
    }
}
