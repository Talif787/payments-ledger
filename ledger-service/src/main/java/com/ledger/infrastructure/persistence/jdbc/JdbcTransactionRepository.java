package com.ledger.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.application.port.out.TransactionRepository;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import com.ledger.domain.transaction.LedgerTransaction;
import com.ledger.domain.transaction.Posting;
import com.ledger.domain.transaction.PostingDirection;
import com.ledger.domain.transaction.TransactionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {

    private static final TypeReference<Map<String, String>> META_TYPE = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcTransactionRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void persist(LedgerTransaction tx) {
        String insertTx = """
                INSERT INTO transactions (id, currency, metadata, created_at)
                VALUES (:id, :currency, CAST(:metadata AS jsonb), :createdAt)
                """;
        jdbc.update(insertTx, new MapSqlParameterSource()
                .addValue("id", tx.id().value())
                .addValue("currency", tx.currency().name())
                .addValue("metadata", toJson(tx.metadata()))
                .addValue("createdAt", java.sql.Timestamp.from(tx.createdAt())));

        String insertPosting = """
                INSERT INTO postings (transaction_id, account_id, direction, minor_units, currency, created_at)
                VALUES (:txId, :accountId, :direction, :minorUnits, :currency, :createdAt)
                """;
        List<MapSqlParameterSource> batch = new ArrayList<>();
        for (Posting p : tx.postings()) {
            batch.add(new MapSqlParameterSource()
                    .addValue("txId", tx.id().value())
                    .addValue("accountId", p.accountId().value())
                    .addValue("direction", p.direction().name())
                    .addValue("minorUnits", p.amount().minorUnits())
                    .addValue("currency", p.amount().currency().name())
                    .addValue("createdAt", java.sql.Timestamp.from(tx.createdAt())));
        }
        jdbc.batchUpdate(insertPosting, batch.toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public Optional<LedgerTransaction> findById(TransactionId id) {
        String txSql = "SELECT id, currency, metadata, created_at FROM transactions WHERE id = :id";
        var header = jdbc.query(txSql, new MapSqlParameterSource("id", id.value()), rs -> {
            if (!rs.next()) {
                return null;
            }
            return new Object[]{
                    Currency.valueOf(rs.getString("currency")),
                    fromJson(rs.getString("metadata")),
                    rs.getTimestamp("created_at").toInstant()
            };
        });
        if (header == null) {
            return Optional.empty();
        }

        String postingsSql = """
                SELECT account_id, direction, minor_units, currency
                FROM postings WHERE transaction_id = :id ORDER BY id
                """;
        List<Posting> postings = new ArrayList<>();
        Currency currency = (Currency) header[0];
        jdbc.query(postingsSql, new MapSqlParameterSource("id", id.value()), rs -> {
            postings.add(new Posting(
                    new AccountId(rs.getObject("account_id", UUID.class)),
                    PostingDirection.valueOf(rs.getString("direction")),
                    Money.ofMinor(rs.getLong("minor_units"), Currency.valueOf(rs.getString("currency")))));
        });

        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) header[1];
        return Optional.of(LedgerTransaction.create(
                id, postings, metadata, (java.time.Instant) header[2]));
    }

    private String toJson(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize transaction metadata", e);
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            return json == null ? Map.of() : objectMapper.readValue(json, META_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize transaction metadata", e);
        }
    }
}
