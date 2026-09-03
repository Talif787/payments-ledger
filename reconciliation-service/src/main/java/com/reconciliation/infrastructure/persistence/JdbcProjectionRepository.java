package com.reconciliation.infrastructure.persistence;

import com.reconciliation.application.port.ProjectionStore;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjectionRepository implements ProjectionStore {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProjectionRepository(@Qualifier("reconciliationJdbc") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markConsumed(UUID eventId) {
        int inserted = jdbc.update(
                "INSERT INTO consumed_events (event_id) VALUES (:id) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource("id", eventId));
        return inserted == 1;
    }

    @Override
    public void applyDelta(String accountId, long deltaMinor) {
        String sql = """
                INSERT INTO derived_balances (account_id, minor_units, updated_at)
                VALUES (:accountId, :delta, now())
                ON CONFLICT (account_id) DO UPDATE
                SET minor_units = derived_balances.minor_units + EXCLUDED.minor_units,
                    updated_at = now()
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("delta", deltaMinor));
    }

    @Override
    public Map<String, Long> derivedBalances() {
        return jdbc.query("SELECT account_id, minor_units FROM derived_balances",
                        (rs, rowNum) -> Map.entry(rs.getString("account_id"), rs.getLong("minor_units")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
