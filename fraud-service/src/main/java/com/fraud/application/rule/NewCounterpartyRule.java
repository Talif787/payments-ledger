package com.fraud.application.rule;

import com.fraud.domain.Decision;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/** A large transfer to a counterparty never seen before is worth a review. */
public final class NewCounterpartyRule implements Rule {

    private final long amountThresholdMinor;

    public NewCounterpartyRule(long amountThresholdMinor) {
        this.amountThresholdMinor = amountThresholdMinor;
    }

    @Override
    public RuleOutcome evaluate(TransactionContext ctx, RawFeatures f) {
        if (f.newCounterparty() && ctx.amountMinor() >= amountThresholdMinor) {
            return RuleOutcome.triggered("NEW_COUNTERPARTY_LARGE", Decision.REVIEW,
                    "new counterparty with amount " + ctx.amountMinor() + " >= " + amountThresholdMinor);
        }
        return RuleOutcome.notTriggered("NEW_COUNTERPARTY_LARGE");
    }
}
