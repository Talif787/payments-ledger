package com.ledger.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the event topics for local and non-production environments. In
 * production, topics are provisioned by infrastructure-as-code with explicit
 * partition and replication settings rather than auto-created here.
 */
@Configuration
@ConditionalOnProperty(name = "ledger.relay.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfig {

    @Bean
    public NewTopic accountsTopic(@Value("${ledger.relay.accounts-topic}") String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic transactionsTopic(@Value("${ledger.relay.transactions-topic}") String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }
}
