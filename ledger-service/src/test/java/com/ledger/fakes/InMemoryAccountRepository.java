package com.ledger.fakes;

import com.ledger.application.port.out.AccountRepository;
import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> store = new ConcurrentHashMap<>();

    @Override public Optional<Account> findById(AccountId id) { return Optional.ofNullable(store.get(id)); }
    @Override public void save(Account account) { store.put(account.id(), account); }
}
