package com.ledger.presentation.web;

import com.ledger.application.port.in.PostTransactionUseCase;
import com.ledger.presentation.web.fraud.FraudGate;
import com.ledger.presentation.web.dto.BalanceDto;
import com.ledger.presentation.web.dto.PostTransactionRequest;
import com.ledger.presentation.web.dto.PostTransactionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final PostTransactionUseCase postTransaction;
    private final FraudGate fraudGate;

    public TransactionController(PostTransactionUseCase postTransaction, FraudGate fraudGate) {
        this.postTransaction = postTransaction;
        this.fraudGate = fraudGate;
    }

    @PostMapping
    public ResponseEntity<PostTransactionResponse> post(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PostTransactionRequest request) {

        fraudGate.check(request);

        List<PostTransactionUseCase.PostingLine> lines = request.postings().stream()
                .map(p -> new PostTransactionUseCase.PostingLine(
                        p.accountId(), p.direction(), p.amount(), p.currency()))
                .toList();

        var result = postTransaction.post(
                new PostTransactionUseCase.Command(idempotencyKey, lines, request.metadata()));

        List<BalanceDto> balances = result.balances().stream()
                .map(b -> new BalanceDto(b.accountId(), b.amount(), b.currency()))
                .toList();

        var body = new PostTransactionResponse(result.transactionId(), result.status(), balances);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
