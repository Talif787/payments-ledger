package com.ledger.presentation.web.dto;

import java.math.BigDecimal;

public record BalanceResponse(String accountId, BigDecimal amount, String currency) {
}
