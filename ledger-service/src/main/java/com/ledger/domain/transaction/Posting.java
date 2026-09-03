package com.ledger.domain.transaction;

import com.ledger.domain.account.AccountId;
import com.ledger.domain.money.Money;
import java.util.Objects;

/**
 * A single immutable line of a transaction: a positive amount applied to one
 * account in one direction. The signed effect on the account balance follows
 * the credit-positive convention.
 */
public record Posting(AccountId accountId, PostingDirection direction, Money amount) {

    public Posting {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(amount, "amount");
        amount.requirePositive();
    }

    /** The change this posting applies to its account's balance. */
    public Money signedEffect() {
        return direction == PostingDirection.CREDIT ? amount : amount.negate();
    }
}
