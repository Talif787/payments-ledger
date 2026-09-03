package com.ledger.fakes;

import com.ledger.application.model.AccountBalance;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.domain.account.AccountId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBalanceRepository implements BalanceRepository {
    private final Map<AccountId, AccountBalance> store = new ConcurrentHashMap<>();

    @Override
    public Map<AccountId, AccountBalance> findForUpdate(Set<AccountId> ids) {
        Map<AccountId, AccountBalance> result = new HashMap<>();
        for (AccountId id : ids) {
            AccountBalance b = store.get(id);
            if (b != null) result.put(id, b);
        }
        return result;
    }

    @Override public Optional<AccountBalance> findById(AccountId id) { return Optional.ofNullable(store.get(id)); }
    @Override public void save(AccountBalance balance) { store.put(balance.accountId(), balance); }
}
