package com.ledger.infrastructure.fraud;

import com.ledger.application.model.FraudVerdict;
import com.ledger.application.port.out.FraudEvaluator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the fraud service over HTTP with a strict timeout. Any failure (timeout,
 * connection refused, non-2xx) returns an unscreened ALLOW so the ledger is never
 * blocked by a fraud-side problem. Only created when ledger.fraud.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "ledger.fraud.enabled", havingValue = "true")
public class HttpFraudEvaluator implements FraudEvaluator {

    private static final Logger log = LoggerFactory.getLogger(HttpFraudEvaluator.class);

    private final RestClient client;

    public HttpFraudEvaluator(@Value("${ledger.fraud.url:http://localhost:8082}") String baseUrl,
                              @Value("${ledger.fraud.timeout-ms:150}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public FraudVerdict evaluate(String accountId, long amountMinor, String currency, String counterpartyId) {
        try {
            Map<String, Object> req = Map.of(
                    "accountId", accountId,
                    "amountMinor", amountMinor,
                    "currency", currency,
                    "counterpartyId", counterpartyId == null ? "" : counterpartyId);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = client.post().uri("/v1/fraud/evaluate")
                    .body(req).retrieve().body(Map.class);
            if (resp == null || resp.get("decision") == null) {
                return FraudVerdict.allowUnscreened();
            }
            double score = resp.get("modelScore") instanceof Number n ? n.doubleValue() : 0.0;
            return new FraudVerdict(resp.get("decision").toString(), score, true);
        } catch (Exception e) {
            log.warn("Fraud screening unavailable, allowing unscreened: {}", e.getMessage());
            return FraudVerdict.allowUnscreened();
        }
    }
}
