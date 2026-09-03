package com.fraud.domain;

import java.time.Instant;

/**
 * The proposed money movement being screened. Amounts are integer minor units,
 * consistent with the ledger. counterpartyId may be null.
 */
public record TransactionContext(
        String accountId,
        long amountMinor,
        String currency,
        String counterpartyId,
        Instant at) {
}
