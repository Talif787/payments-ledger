package com.ledger.domain.transaction;

import com.ledger.domain.account.AccountId;
import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The transaction aggregate: an immutable set of postings that is guaranteed,
 * at construction time, to be a valid double-entry transaction. Invariants
 * enforced here (and therefore true for every instance that exists):
 * at least two postings, a single currency, and total credits equal to total
 * debits so the transaction conserves money. Construction is the only place
 * these rules live, so no service can create an unbalanced transaction.
 */
public final class LedgerTransaction {

    private static final int MIN_POSTINGS = 2;

    private final TransactionId id;
    private final List<Posting> postings;
    private final Currency currency;
    private final Map<String, String> metadata;
    private final Instant createdAt;

    private LedgerTransaction(TransactionId id, List<Posting> postings, Currency currency,
                             Map<String, String> metadata, Instant createdAt) {
        this.id = id;
        this.postings = postings;
        this.currency = currency;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static LedgerTransaction create(TransactionId id, List<Posting> postings,
                                          Map<String, String> metadata, Instant createdAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(postings, "postings");
        Objects.requireNonNull(createdAt, "createdAt");

        if (postings.size() < MIN_POSTINGS) {
            throw new DomainException.InvalidTransaction(
                    "A transaction requires at least " + MIN_POSTINGS + " postings");
        }

        Currency currency = postings.get(0).amount().currency();
        boolean singleCurrency = postings.stream()
                .allMatch(p -> p.amount().currency() == currency);
        if (!singleCurrency) {
            throw new DomainException.CurrencyMismatch(
                    "All postings in a transaction must share one currency");
        }

        Money net = postings.stream()
                .map(Posting::signedEffect)
                .reduce(Money.zero(currency), Money::plus);
        if (!net.isZero()) {
            throw new DomainException.UnbalancedTransaction(
                    "Debits and credits do not balance; net = " + net);
        }

        List<Posting> frozenPostings = List.copyOf(postings);
        Map<String, String> frozenMeta = metadata == null ? Map.of() : Map.copyOf(metadata);
        return new LedgerTransaction(id, frozenPostings, currency, frozenMeta, createdAt);
    }

    public TransactionId id() { return id; }
    public List<Posting> postings() { return postings; }
    public Currency currency() { return currency; }
    public Map<String, String> metadata() { return metadata; }
    public Instant createdAt() { return createdAt; }

    public Set<AccountId> affectedAccountIds() {
        return postings.stream().map(Posting::accountId).collect(Collectors.toUnmodifiableSet());
    }
}
