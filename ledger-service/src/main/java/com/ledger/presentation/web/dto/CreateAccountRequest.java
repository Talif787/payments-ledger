package com.ledger.presentation.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "currency is required") String currency,
        boolean allowOverdraft) {
}
