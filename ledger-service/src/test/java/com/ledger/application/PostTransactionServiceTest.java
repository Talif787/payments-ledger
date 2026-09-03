package com.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.application.port.in.CreateAccountUseCase;
import com.ledger.application.port.in.PostTransactionUseCase;
import com.ledger.application.port.in.PostTransactionUseCase.Command;
import com.ledger.application.port.in.PostTransactionUseCase.PostingLine;
import com.ledger.application.service.CreateAccountService;
import com.ledger.application.service.GetBalanceService;
import com.ledger.application.service.PostTransactionService;
import com.ledger.domain.exception.DomainException;
import com.ledger.fakes.DirectTransactionRunner;
import com.ledger.fakes.InMemoryAccountRepository;
import com.ledger.fakes.InMemoryBalanceRepository;
import com.ledger.fakes.InMemoryIdempotencyRepository;
import com.ledger.fakes.InMemoryOutboxRepository;
import com.ledger.fakes.InMemoryTransactionRepository;
import com.ledger.fakes.RandomIdGenerator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostTransactionServiceTest {

    private InMemoryOutboxRepository outbox;
    private CreateAccountService createAccount;
    private PostTransactionService post;
    private GetBalanceService getBalance;

    private String house;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() {
        var accounts = new InMemoryAccountRepository();
        var balances = new InMemoryBalanceRepository();
        var txs = new InMemoryTransactionRepository();
        var idem = new InMemoryIdempotencyRepository();
        outbox = new InMemoryOutboxRepository();
        var runner = new DirectTransactionRunner();
        var ids = new RandomIdGenerator();
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

        createAccount = new CreateAccountService(accounts, balances, outbox, runner, ids, clock);
        post = new PostTransactionService(accounts, balances, txs, idem, outbox, runner, ids, clock);
        getBalance = new GetBalanceService(accounts, balances);

        house = createAccount.create(new CreateAccountUseCase.Command("USD", true)).accountId();
        alice = createAccount.create(new CreateAccountUseCase.Command("USD", false)).accountId();
        bob = createAccount.create(new CreateAccountUseCase.Command("USD", false)).accountId();
        fund(alice, "100.00");
    }

    @Test
    void postsBalancedTransferAndUpdatesBalances() {
        var result = post.post(transfer("transfer-1", alice, bob, "10.00"));

        assertThat(result.status()).isEqualTo("POSTED");
        assertThat(getBalance.getBalance(alice).amount()).isEqualByComparingTo("90.00");
        assertThat(getBalance.getBalance(bob).amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void replaysIdenticalRequestExactlyOnce() {
        var first = post.post(transfer("transfer-1", alice, bob, "10.00"));
        int eventsAfterFirst = outbox.events().size();

        var replay = post.post(transfer("transfer-1", alice, bob, "10.00"));

        assertThat(replay.transactionId()).isEqualTo(first.transactionId());
        assertThat(outbox.events()).hasSize(eventsAfterFirst);
        assertThat(getBalance.getBalance(alice).amount()).isEqualByComparingTo("90.00");
    }

    @Test
    void rejectsSameKeyWithDifferentRequest() {
        post.post(transfer("transfer-1", alice, bob, "10.00"));
        assertThatThrownBy(() -> post.post(transfer("transfer-1", alice, bob, "20.00")))
                .isInstanceOf(DomainException.IdempotencyConflict.class);
    }

    @Test
    void rejectsInsufficientFunds() {
        assertThatThrownBy(() -> post.post(transfer("overspend-1", bob, alice, "5.00")))
                .isInstanceOf(DomainException.InsufficientFunds.class);
    }

    @Test
    void rejectsUnbalancedRequest() {
        var command = new Command("bad-1",
                List.of(
                        new PostingLine(alice, "DEBIT", new BigDecimal("10.00"), "USD"),
                        new PostingLine(bob, "CREDIT", new BigDecimal("9.00"), "USD")),
                Map.of());
        assertThatThrownBy(() -> post.post(command))
                .isInstanceOf(DomainException.UnbalancedTransaction.class);
    }

    private void fund(String account, String amount) {
        post.post(new Command("fund-" + account,
                List.of(
                        new PostingLine(house, "DEBIT", new BigDecimal(amount), "USD"),
                        new PostingLine(account, "CREDIT", new BigDecimal(amount), "USD")),
                Map.of("reason", "test funding")));
    }

    private Command transfer(String key, String from, String to, String amount) {
        return new Command(key,
                List.of(
                        new PostingLine(from, "DEBIT", new BigDecimal(amount), "USD"),
                        new PostingLine(to, "CREDIT", new BigDecimal(amount), "USD")),
                Map.of());
    }
}
