package com.ledger.presentation.web.dto;

import java.math.BigDecimal;

public record BalanceDto(String accountId, BigDecimal amount, String currency) {
}
