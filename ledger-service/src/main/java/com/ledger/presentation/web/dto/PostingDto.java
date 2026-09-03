package com.ledger.presentation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PostingDto(
        @NotBlank(message = "accountId is required") String accountId,
        @NotBlank(message = "direction is required") String direction,
        @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount,
        @NotBlank(message = "currency is required") String currency) {
}
