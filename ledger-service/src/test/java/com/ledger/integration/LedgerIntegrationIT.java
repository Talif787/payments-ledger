package com.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.presentation.web.dto.BalanceResponse;
import com.ledger.presentation.web.dto.CreateAccountRequest;
import com.ledger.presentation.web.dto.CreateAccountResponse;
import com.ledger.presentation.web.dto.PostTransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-stack test against a real PostgreSQL (Testcontainers), covering the
 * migration, the balanced-transaction trigger, and exactly-once semantics
 * through the HTTP API. Requires Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LedgerIntegrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ledger").withUsername("ledger").withPassword("ledger");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void fundsTransfersAndIsIdempotent() {
        String house = createAccount("USD", true);
        String alice = createAccount("USD", false);
        String bob = createAccount("USD", false);

        postTransaction("fund-alice", house, alice, "100.00", HttpStatus.CREATED);

        var first = postTransaction("transfer-1", alice, bob, "10.00", HttpStatus.CREATED);
        var replay = postTransaction("transfer-1", alice, bob, "10.00", HttpStatus.CREATED);
        assertThat(replay.getBody().transactionId()).isEqualTo(first.getBody().transactionId());

        assertThat(balance(alice)).isEqualByComparingTo("90.00");
        assertThat(balance(bob)).isEqualByComparingTo("10.00");
    }

    private String createAccount(String currency, boolean overdraft) {
        var response = rest.postForEntity("/v1/accounts",
                new CreateAccountRequest(currency, overdraft), CreateAccountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().accountId();
    }

    private org.springframework.http.ResponseEntity<PostTransactionResponse> postTransaction(
            String key, String from, String to, String amount, HttpStatus expected) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        var body = Map.of("postings", List.of(
                Map.of("accountId", from, "direction", "DEBIT", "amount", amount, "currency", "USD"),
                Map.of("accountId", to, "direction", "CREDIT", "amount", amount, "currency", "USD")));
        var response = rest.exchange("/v1/transactions", HttpMethod.POST,
                new HttpEntity<>(body, headers), PostTransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response;
    }

    private BigDecimal balance(String accountId) {
        return rest.getForObject("/v1/accounts/" + accountId + "/balance", BalanceResponse.class).amount();
    }
}
