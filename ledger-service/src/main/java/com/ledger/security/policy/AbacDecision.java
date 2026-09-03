package com.ledger.security.policy;

/** The outcome of an attribute-based check. reason is null when permitted. */
public record AbacDecision(boolean permitted, String reason) {

    public static AbacDecision permit() {
        return new AbacDecision(true, null);
    }

    public static AbacDecision deny(String reason) {
        return new AbacDecision(false, reason);
    }
}
