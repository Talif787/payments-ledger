package com.ledger.infrastructure.persistence.jdbc;

import com.ledger.application.model.AccountBalance;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBalanceRepository implements BalanceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBalanceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<AccountId, AccountBalance> findForUpdate(Set<AccountId> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = accountIds.stream().map(AccountId::value).collect(Collectors.toList());
        String sql = """
                SELECT account_id, minor_units, currency, version
                FROM account_balances
                WHERE account_id IN (:ids)
                FOR UPDATE
                """;
        var params = new MapSqlParameterSource("ids", ids);
        Map<AccountId, AccountBalance> result = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            AccountId id = new AccountId(rs.getObject("account_id", UUID.class));
            Money money = Money.ofMinor(rs.getLong("minor_units"), Currency.valueOf(rs.getString("currency")));
            result.put(id, new AccountBalance(id, money, rs.getLong("version")));
        });
        return result;
    }

    @Override
    public Optional<AccountBalance> findById(AccountId accountId) {
        String sql = "SELECT account_id, minor_units, currency, version FROM account_balances WHERE account_id = :id";
        var params = new MapSqlParameterSource("id", accountId.value());
        return jdbc.query(sql, params, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            Money money = Money.ofMinor(rs.getLong("minor_units"), Currency.valueOf(rs.getString("currency")));
            return Optional.of(new AccountBalance(
                    new AccountId(rs.getObject("account_id", UUID.class)), money, rs.getLong("version")));
        });
    }

    @Override
    public void save(AccountBalance balance) {
        String sql = """
                INSERT INTO account_balances (account_id, minor_units, currency, version, updated_at)
                VALUES (:accountId, :minorUnits, :currency, :version, now())
                ON CONFLICT (account_id) DO UPDATE
                SET minor_units = EXCLUDED.minor_units,
                    version     = EXCLUDED.version,
                    updated_at  = now()
                """;
        var params = new MapSqlParameterSource()
                .addValue("accountId", balance.accountId().value())
                .addValue("minorUnits", balance.amount().minorUnits())
                .addValue("currency", balance.amount().currency().name())
                .addValue("version", balance.version());
        jdbc.update(sql, params);
    }
}
