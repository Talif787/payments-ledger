package com.fraud.domain;

import java.time.Instant;

/** One past transaction for an account, as recorded in the feature store. */
public record Activity(Instant at, long amountMinor, String counterpartyId, String txnId) {
}
