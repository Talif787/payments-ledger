package com.fraud.domain;

/**
 * Streaming features for an account as of evaluation time, computed from its
 * recent activity. These are the raw values before normalization into a model
 * feature vector.
 */
public record RawFeatures(
        int txnCount1m,
        int txnCount5m,
        int txnCount1h,
        long amountSum1hMinor,
        int distinctCounterparties1h,
        long secondsSinceLastTxn,   // Long.MAX_VALUE if no prior activity
        boolean newCounterparty) {
}
