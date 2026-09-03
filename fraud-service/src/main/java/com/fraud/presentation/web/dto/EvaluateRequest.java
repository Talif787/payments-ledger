package com.fraud.presentation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** A request to screen a proposed money movement. amountMinor is integer minor units. */
public record EvaluateRequest(
        @NotBlank(message = "accountId is required") String accountId,
        @Positive(message = "amountMinor must be positive") long amountMinor,
        @NotBlank(message = "currency is required") String currency,
        String counterpartyId) {
}
