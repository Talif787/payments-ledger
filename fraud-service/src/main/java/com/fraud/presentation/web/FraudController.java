package com.fraud.presentation.web;

import com.fraud.application.decision.DecisionResult;
import com.fraud.application.service.EvaluateService;
import com.fraud.domain.TransactionContext;
import com.fraud.presentation.web.dto.EvaluateRequest;
import com.fraud.presentation.web.dto.EvaluateResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fraud")
public class FraudController {

    private final EvaluateService evaluateService;
    private final Clock clock;

    public FraudController(EvaluateService evaluateService, Clock clock) {
        this.evaluateService = evaluateService;
        this.clock = clock;
    }

    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
        var ctx = new TransactionContext(
                request.accountId(), request.amountMinor(), request.currency(),
                request.counterpartyId(), clock.instant());
        DecisionResult r = evaluateService.evaluate(ctx);
        return new EvaluateResponse(
                r.effectiveDecision().name(), r.rulesDecision().name(), r.modelDecision().name(),
                r.modelScore(), r.mode().name(), r.reasons());
    }
}
