package com.fraud.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.application.port.FeatureStore;
import com.fraud.domain.Activity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Builds streaming features from the ledger event stream. For each posted
 * transaction, every debit (money leaving an account) is recorded against that
 * account, with the transaction's credit side as the counterparty. Recording is
 * idempotent on transaction id, so at-least-once delivery is safe.
 */
@Component
public class LedgerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventConsumer.class);
    private static final String TRANSACTION_POSTED = "ledger.transaction.posted.v1";

    private final FeatureStore featureStore;
    private final ObjectMapper objectMapper;

    public LedgerEventConsumer(FeatureStore featureStore, ObjectMapper objectMapper) {
        this.featureStore = featureStore;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${fraud.transactions-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        try {
            JsonNode envelope = objectMapper.readTree(payload);
            if (!TRANSACTION_POSTED.equals(envelope.path("eventType").asText())) {
                return;
            }
            String txnId = envelope.path("eventId").asText();
            Instant at = Instant.parse(envelope.path("occurredAt").asText());
            JsonNode data = envelope.path("data");

            List<String> credits = new ArrayList<>();
            for (JsonNode p : data.path("postings")) {
                if ("CREDIT".equals(p.path("direction").asText())) {
                    credits.add(p.path("accountId").asText());
                }
            }
            String counterparty = credits.isEmpty() ? null : credits.get(0);

            for (JsonNode p : data.path("postings")) {
                if ("DEBIT".equals(p.path("direction").asText())) {
                    String account = p.path("accountId").asText();
                    long amount = p.path("minorUnits").asLong();
                    featureStore.recordActivity(account, new Activity(at, amount, counterparty, txnId));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process ledger event for features: {}", e.getMessage());
        }
    }
}
