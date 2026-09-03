package com.fraud.domain;

/**
 * The outcome of a fraud evaluation, ordered by severity so decisions from
 * independent sources (rules, model) can be combined by taking the strictest.
 */
public enum Decision {
    ALLOW(0),
    REVIEW(1),
    BLOCK(2);

    private final int severity;

    Decision(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    /** The stricter of two decisions. */
    public static Decision max(Decision a, Decision b) {
        return a.severity >= b.severity ? a : b;
    }
}
