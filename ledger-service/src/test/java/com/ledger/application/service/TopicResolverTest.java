package com.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TopicResolverTest {

    private final TopicResolver resolver = new TopicResolver("ledger.accounts.v1", "ledger.transactions.v1");

    @Test
    void routesAccountEventsToAccountsTopic() {
        assertThat(resolver.resolve("account.opened.v1")).isEqualTo("ledger.accounts.v1");
    }

    @Test
    void routesTransactionEventsToTransactionsTopic() {
        assertThat(resolver.resolve("ledger.transaction.posted.v1")).isEqualTo("ledger.transactions.v1");
    }

    @Test
    void rejectsUnknownEventType() {
        assertThatThrownBy(() -> resolver.resolve("mystery.v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
