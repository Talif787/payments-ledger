package com.ledger.infrastructure.persistence.jdbc;

import com.ledger.application.model.IdempotencyRecord;
import com.ledger.application.port.out.IdempotencyRepository;
import com.ledger.domain.transaction.IdempotencyKey;
import com.ledger.domain.transaction.TransactionId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcIdempotencyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        String sql = """
                SELECT key, request_fingerprint, transaction_id, status, created_at
                FROM idempotency_keys WHERE key = :key
                """;
        var params = new MapSqlParameterSource("key", key.value());
        return jdbc.query(sql, params, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new IdempotencyRecord(
                    new IdempotencyKey(rs.getString("key")),
                    rs.getString("request_fingerprint"),
                    new TransactionId(rs.getObject("transaction_id", UUID.class)),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant()));
        });
    }

    @Override
    public void save(IdempotencyRecord record) {
        String sql = """
                INSERT INTO idempotency_keys (key, request_fingerprint, transaction_id, status, created_at)
                VALUES (:key, :fingerprint, :txId, :status, :createdAt)
                """;
        var params = new MapSqlParameterSource()
                .addValue("key", record.key().value())
                .addValue("fingerprint", record.requestFingerprint())
                .addValue("txId", record.transactionId().value())
                .addValue("status", record.status())
                .addValue("createdAt", java.sql.Timestamp.from(record.createdAt()));
        jdbc.update(sql, params);
    }
}
