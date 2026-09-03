package com.ledger.domain.account;

import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Currency;
import java.util.Objects;

/**
 * A ledger account. An account has a fixed currency and a policy controlling
 * whether its balance may go negative. Business behaviour that depends only on
 * the account's own state lives here rather than in a service.
 */
public record Account(
        AccountId id,
        Currency currency,
        AccountStatus status,
        boolean allowOverdraft) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(status, "status");
    }

    public static Account open(AccountId id, Currency currency, boolean allowOverdraft) {
        return new Account(id, currency, AccountStatus.ACTIVE, allowOverdraft);
    }

    public void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new DomainException.AccountInactive("Account " + id + " is " + status);
        }
    }
}
