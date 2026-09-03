package com.fraud.infrastructure.persistence;

import com.fraud.application.port.DecisionLog;
import com.fraud.application.service.DecisionRecord;
import java.sql.Timestamp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDecisionLog implements DecisionLog {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDecisionLog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(DecisionRecord r) {
        String sql = """
                INSERT INTO fraud_decisions
                    (decided_at, account_id, amount_minor, currency, counterparty_id, transaction_id,
                     effective_decision, rules_decision, model_decision, model_score, mode, reasons)
                VALUES
                    (:decidedAt, :accountId, :amountMinor, :currency, :counterpartyId, :transactionId,
                     :effective, :rules, :model, :score, :mode, :reasons)
                """;
        var p = new MapSqlParameterSource()
                .addValue("decidedAt", Timestamp.from(r.at()))
                .addValue("accountId", r.context().accountId())
                .addValue("amountMinor", r.context().amountMinor())
                .addValue("currency", r.context().currency())
                .addValue("counterpartyId", r.context().counterpartyId())
                .addValue("transactionId", null)
                .addValue("effective", r.result().effectiveDecision().name())
                .addValue("rules", r.result().rulesDecision().name())
                .addValue("model", r.result().modelDecision().name())
                .addValue("score", r.result().modelScore())
                .addValue("mode", r.result().mode().name())
                .addValue("reasons", String.join("; ", r.result().reasons()));
        jdbc.update(sql, p);
    }
}
