package com.ledger.infrastructure.config;

import com.ledger.application.port.out.AccountRepository;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.application.port.out.IdGenerator;
import com.ledger.application.port.out.IdempotencyRepository;
import com.ledger.application.port.out.OutboxRepository;
import com.ledger.application.port.out.TransactionRepository;
import com.ledger.application.port.out.TransactionRunner;
import com.ledger.application.service.CreateAccountService;
import com.ledger.application.service.GetBalanceService;
import com.ledger.application.service.PostTransactionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free application services as Spring beans. Keeping this
 * wiring in the infrastructure layer lets the application and domain layers stay
 * free of any framework dependency, which is what allows them to be unit tested
 * (and compiled) in isolation.
 */
@Configuration
public class BeansConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CreateAccountService createAccountService(AccountRepository accounts,
                                                     BalanceRepository balances,
                                                     OutboxRepository outbox,
                                                     TransactionRunner runner,
                                                     IdGenerator ids,
                                                     Clock clock) {
        return new CreateAccountService(accounts, balances, outbox, runner, ids, clock);
    }

    @Bean
    public GetBalanceService getBalanceService(AccountRepository accounts, BalanceRepository balances) {
        return new GetBalanceService(accounts, balances);
    }

    @Bean
    public PostTransactionService postTransactionService(AccountRepository accounts,
                                                         BalanceRepository balances,
                                                         TransactionRepository transactions,
                                                         IdempotencyRepository idempotency,
                                                         OutboxRepository outbox,
                                                         TransactionRunner runner,
                                                         IdGenerator ids,
                                                         Clock clock) {
        return new PostTransactionService(
                accounts, balances, transactions, idempotency, outbox, runner, ids, clock);
    }
}
