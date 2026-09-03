package com.ledger.application.service;

import com.ledger.application.model.AccountBalance;
import com.ledger.application.port.in.CreateAccountUseCase;
import com.ledger.application.port.out.AccountRepository;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.application.port.out.IdGenerator;
import com.ledger.application.port.out.OutboxRepository;
import com.ledger.application.port.out.TransactionRunner;
import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.event.AccountOpened;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import java.time.Clock;

public final class CreateAccountService implements CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final OutboxRepository outboxRepository;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CreateAccountService(AccountRepository accountRepository,
                                BalanceRepository balanceRepository,
                                OutboxRepository outboxRepository,
                                TransactionRunner transactionRunner,
                                IdGenerator idGenerator,
                                Clock clock) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.outboxRepository = outboxRepository;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public Result create(Command command) {
        Currency currency = Currency.of(command.currency());
        AccountId id = idGenerator.newAccountId();
        Account account = Account.open(id, currency, command.allowOverdraft());

        return transactionRunner.inSerializableTransaction(() -> {
            accountRepository.save(account);
            balanceRepository.save(new AccountBalance(id, Money.zero(currency), 0L));
            outboxRepository.append(new AccountOpened(
                    id.toString(), currency.name(), account.allowOverdraft(), clock.instant()));
            return new Result(id.toString(), currency.name(), account.status().name());
        });
    }
}
