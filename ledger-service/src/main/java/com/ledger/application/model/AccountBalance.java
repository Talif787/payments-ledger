package com.ledger.application.model;

import com.ledger.domain.account.AccountId;
import com.ledger.domain.money.Money;
import java.util.Objects;

/**
 * The balance of an account together with an optimistic-concurrency version.
 * The version guards against lost updates when a balance row is read, mutated,
 * and written back within a transaction.
 */
public record AccountBalance(AccountId accountId, Money amount, long version) {

    public AccountBalance {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(amount, "amount");
    }

    public AccountBalance apply(Money delta) {
        return new AccountBalance(accountId, amount.plus(delta), version + 1);
    }
}
