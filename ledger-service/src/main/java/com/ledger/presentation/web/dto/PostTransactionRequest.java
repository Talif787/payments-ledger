package com.ledger.presentation.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record PostTransactionRequest(
        @NotEmpty(message = "at least two postings are required")
        @Size(min = 2, message = "at least two postings are required")
        @Valid List<PostingDto> postings,
        Map<String, String> metadata) {
}
