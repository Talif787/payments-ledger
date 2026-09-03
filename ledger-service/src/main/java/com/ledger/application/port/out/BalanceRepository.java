package com.ledger.application.port.out;

import com.ledger.application.model.AccountBalance;
import com.ledger.domain.account.AccountId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BalanceRepository {

    /**
     * Reads the balances for the given accounts, acquiring a row-level write
     * lock on each (SELECT ... FOR UPDATE) so concurrent transactions touching
     * the same accounts serialize rather than race.
     */
    Map<AccountId, AccountBalance> findForUpdate(Set<AccountId> accountIds);

    Optional<AccountBalance> findById(AccountId accountId);

    void save(AccountBalance balance);
}
