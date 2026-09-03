package com.ledger.application.port.in;

import java.math.BigDecimal;

public interface GetBalanceUseCase {

    Result getBalance(String accountId);

    record Result(String accountId, BigDecimal amount, String currency) {}
}
