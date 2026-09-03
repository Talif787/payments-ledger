package com.fraud.application.rule;

import com.fraud.domain.Decision;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/** Flags when this transaction plus the last hour's spend crosses a ceiling. */
public final class SpendSpikeRule implements Rule {

    private final long hourlySpendCapMinor;

    public SpendSpikeRule(long hourlySpendCapMinor) {
        this.hourlySpendCapMinor = hourlySpendCapMinor;
    }

    @Override
    public RuleOutcome evaluate(TransactionContext ctx, RawFeatures f) {
        long projected = f.amountSum1hMinor() + ctx.amountMinor();
        if (projected >= hourlySpendCapMinor) {
            return RuleOutcome.triggered("SPEND_SPIKE_1H", Decision.REVIEW,
                    "1h spend " + projected + " >= cap " + hourlySpendCapMinor);
        }
        return RuleOutcome.notTriggered("SPEND_SPIKE_1H");
    }
}
