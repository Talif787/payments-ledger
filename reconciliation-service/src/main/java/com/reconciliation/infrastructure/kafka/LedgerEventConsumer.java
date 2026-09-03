package com.reconciliation.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reconciliation.application.service.PostingDelta;
import com.reconciliation.application.service.ProjectionUpdater;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes ledger transaction events and rebuilds each account's balance from the
 * postings inside them, independently of the ledger's own balance projection.
 * Deduplication and application commit together in the reconciliation database,
 * so the relay's at-least-once delivery yields exactly-once effects here.
 */
@Component
public class LedgerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventConsumer.class);
    private static final String TRANSACTION_POSTED = "ledger.transaction.posted.v1";

    private final ProjectionUpdater projectionUpdater;
    private final ObjectMapper objectMapper;

    public LedgerEventConsumer(ProjectionUpdater projectionUpdater, ObjectMapper objectMapper) {
        this.projectionUpdater = projectionUpdater;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${recon.transactions-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onMessage(String payload) {
        try {
            JsonNode envelope = objectMapper.readTree(payload);
            String eventType = envelope.path("eventType").asText();
            if (!TRANSACTION_POSTED.equals(eventType)) {
                return;
            }
            UUID eventId = UUID.fromString(envelope.path("eventId").asText());
            List<PostingDelta> deltas = extractDeltas(envelope.path("data"));
            boolean applied = projectionUpdater.apply(eventId, deltas);
            if (!applied) {
                log.debug("Skipped duplicate event {}", eventId);
            }
        } catch (Exception e) {
            // Rethrow so the container retries; dedup makes retries safe.
            throw new IllegalStateException("Failed to process ledger event", e);
        }
    }

    private List<PostingDelta> extractDeltas(JsonNode data) {
        List<PostingDelta> deltas = new ArrayList<>();
        for (JsonNode posting : data.path("postings")) {
            String accountId = posting.path("accountId").asText();
            long minorUnits = posting.path("minorUnits").asLong();
            boolean credit = "CREDIT".equals(posting.path("direction").asText());
            deltas.add(new PostingDelta(accountId, credit ? minorUnits : -minorUnits));
        }
        return deltas;
    }
}
