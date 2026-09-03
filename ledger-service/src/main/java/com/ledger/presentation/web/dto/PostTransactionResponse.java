package com.ledger.presentation.web.dto;

import java.util.List;

public record PostTransactionResponse(String transactionId, String status, List<BalanceDto> balances) {
}
