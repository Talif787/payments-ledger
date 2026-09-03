package com.ledger.infrastructure.relay;

import com.ledger.application.port.out.EventPublisher;
import com.ledger.application.port.out.OutboxReader;
import com.ledger.application.service.OutboxRelayService;
import com.ledger.application.service.TopicResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the framework-free relay service and enables scheduling. Gated so the
 * relay (and its scheduler and Kafka beans) can be turned off in tests and
 * environments without a broker.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ledger.relay.enabled", havingValue = "true", matchIfMissing = true)
public class RelayConfig {

    @Bean
    public TopicResolver topicResolver(
            @Value("${ledger.relay.accounts-topic}") String accountsTopic,
            @Value("${ledger.relay.transactions-topic}") String transactionsTopic) {
        return new TopicResolver(accountsTopic, transactionsTopic);
    }

    @Bean
    public OutboxRelayService outboxRelayService(OutboxReader reader,
                                                 EventPublisher publisher,
                                                 TopicResolver topicResolver) {
        return new OutboxRelayService(reader, publisher, topicResolver);
    }
}
