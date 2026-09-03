package com.ledger.application.service;

/**
 * Maps a domain event type to the log topic it belongs on. Keeping this pure and
 * explicit (rather than deriving topics implicitly) means the routing is
 * testable and the set of topics is easy to audit.
 */
public final class TopicResolver {

    private final String accountsTopic;
    private final String transactionsTopic;

    public TopicResolver(String accountsTopic, String transactionsTopic) {
        this.accountsTopic = accountsTopic;
        this.transactionsTopic = transactionsTopic;
    }

    public String resolve(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (eventType.startsWith("account.")) {
            return accountsTopic;
        }
        if (eventType.startsWith("ledger.transaction.")) {
            return transactionsTopic;
        }
        throw new IllegalArgumentException("No topic mapping for event type: " + eventType);
    }
}
