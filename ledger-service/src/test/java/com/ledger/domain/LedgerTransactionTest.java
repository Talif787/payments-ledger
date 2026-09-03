package com.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.domain.account.AccountId;
import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import com.ledger.domain.transaction.LedgerTransaction;
import com.ledger.domain.transaction.Posting;
import com.ledger.domain.transaction.PostingDirection;
import com.ledger.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class LedgerTransactionTest {

    private final AccountId a = new AccountId(UUID.randomUUID());
    private final AccountId b = new AccountId(UUID.randomUUID());

    @Test
    void acceptsBalancedTransaction() {
        LedgerTransaction tx = LedgerTransaction.create(
                new TransactionId(UUID.randomUUID()),
                List.of(
                        new Posting(a, PostingDirection.DEBIT, Money.ofMinor(1000, Currency.USD)),
                        new Posting(b, PostingDirection.CREDIT, Money.ofMinor(1000, Currency.USD))),
                Map.of(), Instant.now());
        assertThat(tx.affectedAccountIds()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void rejectsUnbalancedTransaction() {
        assertThatThrownBy(() -> LedgerTransaction.create(
                new TransactionId(UUID.randomUUID()),
                List.of(
                        new Posting(a, PostingDirection.DEBIT, Money.ofMinor(1000, Currency.USD)),
                        new Posting(b, PostingDirection.CREDIT, Money.ofMinor(999, Currency.USD))),
                Map.of(), Instant.now()))
                .isInstanceOf(DomainException.UnbalancedTransaction.class);
    }

    @Test
    void rejectsFewerThanTwoPostings() {
        assertThatThrownBy(() -> LedgerTransaction.create(
                new TransactionId(UUID.randomUUID()),
                List.of(new Posting(a, PostingDirection.DEBIT, Money.ofMinor(1000, Currency.USD))),
                Map.of(), Instant.now()))
                .isInstanceOf(DomainException.InvalidTransaction.class);
    }

    @Test
    void rejectsMixedCurrencies() {
        assertThatThrownBy(() -> LedgerTransaction.create(
                new TransactionId(UUID.randomUUID()),
                List.of(
                        new Posting(a, PostingDirection.DEBIT, Money.ofMinor(1000, Currency.USD)),
                        new Posting(b, PostingDirection.CREDIT, Money.ofMinor(1000, Currency.EUR))),
                Map.of(), Instant.now()))
                .isInstanceOf(DomainException.CurrencyMismatch.class);
    }

    /**
     * Property: any transaction built from a random set of debits and an equal
     * total of credits is accepted, and its signed effects always sum to zero.
     */
    @RepeatedTest(50)
    void randomBalancedTransactionsAreAccepted() {
        Random random = new Random();
        int legs = 1 + random.nextInt(5);
        long total = 0;
        List<Posting> postings = new ArrayList<>();
        for (int i = 0; i < legs; i++) {
            long amount = 1 + random.nextInt(100_000);
            total += amount;
            postings.add(new Posting(new AccountId(UUID.randomUUID()),
                    PostingDirection.DEBIT, Money.ofMinor(amount, Currency.USD)));
        }
        postings.add(new Posting(new AccountId(UUID.randomUUID()),
                PostingDirection.CREDIT, Money.ofMinor(total, Currency.USD)));

        LedgerTransaction tx = LedgerTransaction.create(
                new TransactionId(UUID.randomUUID()), postings, Map.of(), Instant.now());

        long net = tx.postings().stream().mapToLong(p -> p.signedEffect().minorUnits()).sum();
        assertThat(net).isZero();
    }
}
