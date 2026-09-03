package com.ledger.presentation.web.fraud;

import com.ledger.application.model.FraudVerdict;
import com.ledger.application.port.out.FraudEvaluator;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import com.ledger.presentation.web.dto.PostTransactionRequest;
import com.ledger.presentation.web.dto.PostingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authorization-time fraud gate at the API boundary, before the ledger opens its
 * transaction. It screens the largest debit (money leaving an account) and blocks
 * only on an explicit BLOCK verdict. If the fraud service is disabled or
 * unreachable, it allows the transaction (fail-open), so the ledger's
 * availability never depends on the fraud service. This is the CP-ledger /
 * AP-fraud seam in practice.
 */
@Component
public class FraudGate {

    private static final Logger log = LoggerFactory.getLogger(FraudGate.class);

    private final ObjectProvider<FraudEvaluator> evaluatorProvider;

    public FraudGate(ObjectProvider<FraudEvaluator> evaluatorProvider) {
        this.evaluatorProvider = evaluatorProvider;
    }

    public void check(PostTransactionRequest request) {
        FraudEvaluator evaluator = evaluatorProvider.getIfAvailable();
        if (evaluator == null) {
            return; // fraud screening disabled; behave exactly as without it
        }
        try {
            PostingDto debit = largestDebit(request);
            if (debit == null) {
                return;
            }
            String counterparty = firstCreditAccount(request);
            long amountMinor = Money.of(debit.amount(), Currency.of(debit.currency())).minorUnits();

            FraudVerdict verdict = evaluator.evaluate(
                    debit.accountId(), amountMinor, debit.currency(), counterparty);

            if (verdict.blocked()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "FRAUD_BLOCKED: transaction blocked by fraud screening");
            }
        } catch (ResponseStatusException e) {
            throw e; // a real BLOCK; propagate as 403
        } catch (Exception e) {
            // Anything else (conversion, unexpected) fails open to protect availability.
            log.warn("Fraud gate error, allowing transaction: {}", e.getMessage());
        }
    }

    private PostingDto largestDebit(PostTransactionRequest request) {
        PostingDto largest = null;
        for (PostingDto p : request.postings()) {
            if ("DEBIT".equalsIgnoreCase(p.direction())) {
                if (largest == null || p.amount().compareTo(largest.amount()) > 0) {
                    largest = p;
                }
            }
        }
        return largest;
    }

    private String firstCreditAccount(PostTransactionRequest request) {
        for (PostingDto p : request.postings()) {
            if ("CREDIT".equalsIgnoreCase(p.direction())) {
                return p.accountId();
            }
        }
        return null;
    }
}
