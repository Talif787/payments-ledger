package com.reconciliation.infrastructure.persistence;

import com.reconciliation.application.port.LedgerReadPort;
import com.reconciliation.domain.AccountSnapshot;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the ledger's authoritative balances and, independently, the sum of each
 * account's postings, in a single query. Comparing the two columns is how
 * reconciliation catches a balance projection that has drifted from the postings
 * that supposedly produced it.
 */
@Repository
public class JdbcLedgerReadRepository implements LedgerReadPort {

    private final NamedParameterJdbcTemplate ledgerJdbc;

    public JdbcLedgerReadRepository(@Qualifier("ledgerJdbc") NamedParameterJdbcTemplate ledgerJdbc) {
        this.ledgerJdbc = ledgerJdbc;
    }

    @Override
    public List<AccountSnapshot> loadAccountSnapshots() {
        String sql = """
                SELECT b.account_id,
                       b.minor_units AS balance_minor,
                       COALESCE(p.posting_sum, 0) AS posting_sum
                FROM account_balances b
                LEFT JOIN (
                    SELECT account_id,
                           SUM(CASE direction WHEN 'CREDIT' THEN minor_units ELSE -minor_units END) AS posting_sum
                    FROM postings
                    GROUP BY account_id
                ) p ON p.account_id = b.account_id
                """;
        return ledgerJdbc.query(sql, (rs, rowNum) -> new AccountSnapshot(
                rs.getString("account_id"),
                rs.getLong("balance_minor"),
                rs.getLong("posting_sum")));
    }
}
