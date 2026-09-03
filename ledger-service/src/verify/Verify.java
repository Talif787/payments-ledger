package com.ledger.verify;

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

/**
 * Standalone, dependency-free harness used only to verify the framework-free
 * core (domain + application) in an offline sandbox. The delivered test suite is
 * the JUnit code under src/test; this is not part of the build.
 */
public final class Verify {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        var accounts = new InMemoryAccountRepository();
        var balances = new InMemoryBalanceRepository();
        var txs = new InMemoryTransactionRepository();
        var idem = new InMemoryIdempotencyRepository();
        var outbox = new InMemoryOutboxRepository();
        var runner = new DirectTransactionRunner();
        var ids = new RandomIdGenerator();
        var clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

        var createAccount = new CreateAccountService(accounts, balances, outbox, runner, ids, clock);
        var post = new PostTransactionService(accounts, balances, txs, idem, outbox, runner, ids, clock);
        var getBalance = new GetBalanceService(accounts, balances);

        var house = createAccount.create(new CreateAccountUseCase.Command("USD", true));
        var alice = createAccount.create(new CreateAccountUseCase.Command("USD", false));
        var bob = createAccount.create(new CreateAccountUseCase.Command("USD", false));

        check("account creation emits outbox events", outbox.events().size() == 3);

        // Fund Alice from the (overdraft-allowed) house account: 100.00
        post.post(new Command("fund-alice-1",
                List.of(
                        line(house.accountId(), "DEBIT", "100.00"),
                        line(alice.accountId(), "CREDIT", "100.00")),
                Map.of("reason", "initial funding")));
        check("alice funded to 100.00",
                getBalance.getBalance(alice.accountId()).amount().compareTo(new BigDecimal("100.00")) == 0);

        // Transfer 10.00 Alice -> Bob
        var transfer = new Command("transfer-1",
                List.of(
                        line(alice.accountId(), "DEBIT", "10.00"),
                        line(bob.accountId(), "CREDIT", "10.00")),
                Map.of());
        var r1 = post.post(transfer);
        check("transfer status POSTED", r1.status().equals("POSTED"));
        check("alice now 90.00",
                getBalance.getBalance(alice.accountId()).amount().compareTo(new BigDecimal("90.00")) == 0);
        check("bob now 10.00",
                getBalance.getBalance(bob.accountId()).amount().compareTo(new BigDecimal("10.00")) == 0);

        int eventsAfterTransfer = outbox.events().size();

        // Idempotent replay: same key + same request -> same txId, no new effects
        var r2 = post.post(transfer);
        check("idempotent replay returns same transaction id", r1.transactionId().equals(r2.transactionId()));
        check("idempotent replay applied no new balance change",
                getBalance.getBalance(alice.accountId()).amount().compareTo(new BigDecimal("90.00")) == 0);
        check("idempotent replay emitted no new event", outbox.events().size() == eventsAfterTransfer);

        // Idempotency conflict: same key, different request
        check("idempotency conflict detected", throwsDomain(() ->
                post.post(new Command("transfer-1",
                        List.of(line(alice.accountId(), "DEBIT", "5.00"),
                                line(bob.accountId(), "CREDIT", "5.00")), Map.of())),
                "IDEMPOTENCY_CONFLICT"));

        // Unbalanced transaction rejected
        check("unbalanced transaction rejected", throwsDomain(() ->
                post.post(new Command("bad-unbalanced",
                        List.of(line(alice.accountId(), "DEBIT", "10.00"),
                                line(bob.accountId(), "CREDIT", "9.00")), Map.of())),
                "UNBALANCED_TRANSACTION"));

        // Insufficient funds (Bob has 10, no overdraft, tries to send 1000)
        check("insufficient funds rejected", throwsDomain(() ->
                post.post(new Command("bob-overspend",
                        List.of(line(bob.accountId(), "DEBIT", "1000.00"),
                                line(alice.accountId(), "CREDIT", "1000.00")), Map.of())),
                "INSUFFICIENT_FUNDS"));

        // Currency mismatch within a transaction
        check("currency mismatch rejected", throwsDomain(() ->
                post.post(new Command("bad-currency",
                        List.of(new PostingLine(alice.accountId(), "DEBIT", new BigDecimal("1.00"), "USD"),
                                new PostingLine(bob.accountId(), "CREDIT", new BigDecimal("1.00"), "EUR")), Map.of())),
                "CURRENCY_MISMATCH"));

        // Over-precision amount rejected
        check("over-precision amount rejected", throwsDomain(() ->
                post.post(new Command("bad-precision",
                        List.of(new PostingLine(alice.accountId(), "DEBIT", new BigDecimal("1.005"), "USD"),
                                new PostingLine(bob.accountId(), "CREDIT", new BigDecimal("1.005"), "USD")), Map.of())),
                "INVALID_MONEY"));

        // Conservation of money: house + alice + bob == 0 (double-entry invariant across the book)
        long total = minor(getBalance.getBalance(house.accountId()).amount())
                + minor(getBalance.getBalance(alice.accountId()).amount())
                + minor(getBalance.getBalance(bob.accountId()).amount());
        check("system conserves money (net of all accounts is zero)", total == 0);

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static PostingLine line(String accountId, String direction, String amount) {
        return new PostingLine(accountId, direction, new BigDecimal(amount), "USD");
    }

    private static long minor(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private static boolean throwsDomain(Runnable r, String expectedCode) {
        try {
            r.run();
            return false;
        } catch (DomainException e) {
            return e.code().equals(expectedCode);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }
}
