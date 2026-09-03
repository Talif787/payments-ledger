package com.ledger.infrastructure.persistence.jdbc;

import com.ledger.application.port.out.AccountRepository;
import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.account.AccountStatus;
import com.ledger.domain.money.Currency;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAccountRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        String sql = "SELECT id, currency, status, allow_overdraft FROM accounts WHERE id = :id";
        var params = new MapSqlParameterSource("id", id.value());
        return jdbc.query(sql, params, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Account(
                    new AccountId(rs.getObject("id", java.util.UUID.class)),
                    Currency.valueOf(rs.getString("currency")),
                    AccountStatus.valueOf(rs.getString("status")),
                    rs.getBoolean("allow_overdraft")));
        });
    }

    @Override
    public void save(Account account) {
        String sql = """
                INSERT INTO accounts (id, currency, status, allow_overdraft)
                VALUES (:id, :currency, :status, :allowOverdraft)
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", account.id().value())
                .addValue("currency", account.currency().name())
                .addValue("status", account.status().name())
                .addValue("allowOverdraft", account.allowOverdraft());
        jdbc.update(sql, params);
    }
}
