package com.ledger.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface PostTransactionUseCase {

    Result post(Command command);

    record Command(
            String idempotencyKey,
            List<PostingLine> postings,
            Map<String, String> metadata) {
        public Command {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(postings, "postings");
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record PostingLine(String accountId, String direction, BigDecimal amount, String currency) {}

    record Result(String transactionId, String status, List<BalanceView> balances) {}

    record BalanceView(String accountId, BigDecimal amount, String currency) {}
}
