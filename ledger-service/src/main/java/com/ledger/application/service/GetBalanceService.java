package com.ledger.application.service;

import com.ledger.application.model.AccountBalance;
import com.ledger.application.port.in.GetBalanceUseCase;
import com.ledger.application.port.out.AccountRepository;
import com.ledger.application.port.out.BalanceRepository;
import com.ledger.domain.account.Account;
import com.ledger.domain.account.AccountId;
import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Money;

public final class GetBalanceService implements GetBalanceUseCase {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;

    public GetBalanceService(AccountRepository accountRepository, BalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
    }

    @Override
    public Result getBalance(String accountId) {
        AccountId id = AccountId.of(accountId);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new DomainException.AccountNotFound("Account not found: " + id));
        Money amount = balanceRepository.findById(id)
                .map(AccountBalance::amount)
                .orElse(Money.zero(account.currency()));
        return new Result(id.toString(), amount.toBigDecimal(), amount.currency().name());
    }
}
