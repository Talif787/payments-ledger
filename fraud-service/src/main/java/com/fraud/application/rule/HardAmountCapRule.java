package com.fraud.application.rule;

import com.fraud.domain.Decision;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/** Blocks any single transaction at or above a hard ceiling. */
public final class HardAmountCapRule implements Rule {

    private final long capMinor;

    public HardAmountCapRule(long capMinor) {
        this.capMinor = capMinor;
    }

    @Override
    public RuleOutcome evaluate(TransactionContext ctx, RawFeatures features) {
        if (ctx.amountMinor() >= capMinor) {
            return RuleOutcome.triggered("HARD_AMOUNT_CAP", Decision.BLOCK,
                    "amount " + ctx.amountMinor() + " >= cap " + capMinor);
        }
        return RuleOutcome.notTriggered("HARD_AMOUNT_CAP");
    }
}
