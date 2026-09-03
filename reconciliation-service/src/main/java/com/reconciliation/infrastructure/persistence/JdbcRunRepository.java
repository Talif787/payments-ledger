package com.reconciliation.infrastructure.persistence;

import com.reconciliation.application.port.RunStore;
import com.reconciliation.domain.ReconciliationReport;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRunRepository implements RunStore {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRunRepository(@Qualifier("reconciliationJdbc") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(ReconciliationReport report) {
        String sql = """
                INSERT INTO reconciliation_runs (run_at, accounts_checked, discrepancy_count, status)
                VALUES (:runAt, :accountsChecked, :discrepancyCount, :status)
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("runAt", Timestamp.from(report.runAt()))
                .addValue("accountsChecked", report.accountsChecked())
                .addValue("discrepancyCount", report.discrepancies().size())
                .addValue("status", report.status()));
    }
}
