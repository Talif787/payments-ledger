package com.ledger.application.model;

/**
 * The ledger's view of a fraud screening result. screened is false when the
 * fraud service could not be reached in time and the transaction was allowed to
 * proceed (fail-open), which preserves ledger availability per the CP-ledger /
 * AP-fraud seam.
 */
public record FraudVerdict(String decision, double score, boolean screened) {

    public boolean blocked() {
        return "BLOCK".equals(decision);
    }

    public static FraudVerdict allowUnscreened() {
        return new FraudVerdict("ALLOW", 0.0, false);
    }
}
