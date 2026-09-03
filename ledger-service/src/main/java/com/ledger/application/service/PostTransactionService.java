package com.ledger.application.service;

import com.ledger.application.model.AccountBalance;
import com.ledger.application.model.IdempotencyRecord;
import com.ledger.application.port.in.PostTransactionUseCase;
import com.ledger.application.port.out.AccountRepository;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.application.port.out.IdGenerator;
import com.ledger.application.port.out.IdempotencyRepository;
import com.ledger.application.port.out.OutboxRepository;
import com.ledger.application.port.out.TransactionRepository;
import com.ledger.application.port.out.TransactionRunner;
import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.event.TransactionPosted;
import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import com.ledger.domain.transaction.IdempotencyKey;
import com.ledger.domain.transaction.LedgerTransaction;
import com.ledger.domain.transaction.Posting;
import com.ledger.domain.transaction.PostingDirection;
import com.ledger.domain.transaction.TransactionId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Posts a balanced double-entry transaction with exactly-once semantics.
 *
 * The entire operation runs inside one serializable database transaction, which
 * the {@link TransactionRunner} retries on serialization failure. Within it, the
 * idempotency key is checked, affected balances are read under a row lock, the
 * overdraft policy is enforced, and the transaction, updated balances, outbox
 * event, and idempotency record are all persisted atomically. Either every one
 * of these effects happens exactly once or none of them does.
 */
public final class PostTransactionService implements PostTransactionUseCase {

    private static final String STATUS_POSTED = "POSTED";

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public PostTransactionService(AccountRepository accountRepository,
                                  BalanceRepository balanceRepository,
                                  TransactionRepository transactionRepository,
                                  IdempotencyRepository idempotencyRepository,
                                  OutboxRepository outboxRepository,
                                  TransactionRunner transactionRunner,
                                  IdGenerator idGenerator,
                                  Clock clock) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public Result post(Command command) {
        IdempotencyKey key = new IdempotencyKey(command.idempotencyKey());
        List<Posting> postings = toPostings(command.postings());

        return transactionRunner.inSerializableTransaction(() -> {
            TransactionId txId = idGenerator.newTransactionId();
            LedgerTransaction tx = LedgerTransaction.create(txId, postings, command.metadata(), clock.instant());
            String fingerprint = RequestFingerprint.of(tx.currency(), tx.postings(), tx.metadata());

            var existing = idempotencyRepository.find(key);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                if (!record.requestFingerprint().equals(fingerprint)) {
                    throw new DomainException.IdempotencyConflict(
                            "Idempotency key already used with a different request");
                }
                return replayResult(record);
            }

            Set<AccountId> affected = tx.affectedAccountIds();
            Map<AccountId, Account> accounts = loadActiveAccounts(affected, tx.currency());
            Map<AccountId, AccountBalance> balances = balanceRepository.findForUpdate(affected);

            Map<AccountId, Money> deltas = aggregateDeltas(tx, tx.currency());
            List<AccountBalance> updated = new ArrayList<>();
            for (Map.Entry<AccountId, Money> entry : deltas.entrySet()) {
                AccountId accountId = entry.getKey();
                AccountBalance current = balances.get(accountId);
                if (current == null) {
                    throw new DomainException.AccountNotFound("No balance for account " + accountId);
                }
                AccountBalance next = current.apply(entry.getValue());
                if (next.amount().isNegative() && !accounts.get(accountId).allowOverdraft()) {
                    throw new DomainException.InsufficientFunds(
                            "Account " + accountId + " has insufficient funds");
                }
                updated.add(next);
            }

            transactionRepository.persist(tx);
            updated.forEach(balanceRepository::save);
            outboxRepository.append(toEvent(tx));
            idempotencyRepository.save(new IdempotencyRecord(
                    key, fingerprint, txId, STATUS_POSTED, clock.instant()));

            return buildResult(txId.toString(), STATUS_POSTED, updated);
        });
    }

    private Map<AccountId, Account> loadActiveAccounts(Set<AccountId> ids, Currency currency) {
        Map<AccountId, Account> accounts = new HashMap<>();
        for (AccountId id : ids) {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new DomainException.AccountNotFound("Account not found: " + id));
            account.requireActive();
            if (account.currency() != currency) {
                throw new DomainException.CurrencyMismatch(
                        "Account " + id + " is " + account.currency() + " but transaction is " + currency);
            }
            accounts.put(id, account);
        }
        return accounts;
    }

    private Map<AccountId, Money> aggregateDeltas(LedgerTransaction tx, Currency currency) {
        Map<AccountId, Money> deltas = new HashMap<>();
        for (Posting posting : tx.postings()) {
            deltas.merge(posting.accountId(), posting.signedEffect(), Money::plus);
        }
        deltas.replaceAll((id, money) -> money);
        return deltas;
    }

    private List<Posting> toPostings(List<PostingLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainException.InvalidTransaction("At least one posting is required");
        }
        List<Posting> postings = new ArrayList<>(lines.size());
        for (PostingLine line : lines) {
            Currency currency = Currency.of(line.currency());
            PostingDirection direction = parseDirection(line.direction());
            Money amount = Money.of(line.amount(), currency);
            postings.add(new Posting(AccountId.of(line.accountId()), direction, amount));
        }
        return postings;
    }

    private PostingDirection parseDirection(String raw) {
        if (raw == null) {
            throw new DomainException.InvalidTransaction("Posting direction is required");
        }
        try {
            return PostingDirection.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException.InvalidTransaction("Invalid posting direction: " + raw);
        }
    }

    private TransactionPosted toEvent(LedgerTransaction tx) {
        List<TransactionPosted.Entry> entries = tx.postings().stream()
                .map(p -> new TransactionPosted.Entry(
                        p.accountId().toString(), p.direction().name(), p.amount().minorUnits()))
                .toList();
        return new TransactionPosted(
                tx.id().toString(), tx.currency().name(), entries, tx.metadata(), tx.createdAt());
    }

    private Result replayResult(IdempotencyRecord record) {
        LedgerTransaction tx = transactionRepository.findById(record.transactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references missing transaction " + record.transactionId()));
        List<BalanceView> views = new ArrayList<>();
        for (AccountId id : tx.affectedAccountIds()) {
            Money amount = balanceRepository.findById(id)
                    .map(AccountBalance::amount)
                    .orElse(Money.zero(tx.currency()));
            views.add(new BalanceView(id.toString(), amount.toBigDecimal(), amount.currency().name()));
        }
        return new Result(record.transactionId().toString(), record.status(), views);
    }

    private Result buildResult(String txId, String status, List<AccountBalance> balances) {
        List<BalanceView> views = balances.stream()
                .map(b -> new BalanceView(
                        b.accountId().toString(), b.amount().toBigDecimal(), b.amount().currency().name()))
                .toList();
        return new Result(txId, status, views);
    }
}
