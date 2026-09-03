package com.ledger.presentation.web;

import com.ledger.application.port.in.CreateAccountUseCase;
import com.ledger.application.port.in.GetBalanceUseCase;
import com.ledger.presentation.web.dto.BalanceResponse;
import com.ledger.presentation.web.dto.CreateAccountRequest;
import com.ledger.presentation.web.dto.CreateAccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccount;
    private final GetBalanceUseCase getBalance;

    public AccountController(CreateAccountUseCase createAccount, GetBalanceUseCase getBalance) {
        this.createAccount = createAccount;
        this.getBalance = getBalance;
    }

    @PostMapping
    public ResponseEntity<CreateAccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        var result = createAccount.create(
                new CreateAccountUseCase.Command(request.currency(), request.allowOverdraft()));
        var body = new CreateAccountResponse(result.accountId(), result.currency(), result.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> balance(@PathVariable String accountId) {
        var result = getBalance.getBalance(accountId);
        return ResponseEntity.ok(new BalanceResponse(result.accountId(), result.amount(), result.currency()));
    }
}
