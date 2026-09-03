package com.ledger.application.port.out;

import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    void save(Account account);
}
