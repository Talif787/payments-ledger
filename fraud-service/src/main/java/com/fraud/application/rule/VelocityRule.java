package com.fraud.application.rule;

import com.fraud.domain.Decision;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/**
 * Flags bursts. More than reviewThreshold transactions in the last minute is a
 * review; more than blockThreshold is a block.
 */
public final class VelocityRule implements Rule {

    private final int reviewThreshold;
    private final int blockThreshold;

    public VelocityRule(int reviewThreshold, int blockThreshold) {
        this.reviewThreshold = reviewThreshold;
        this.blockThreshold = blockThreshold;
    }

    @Override
    public RuleOutcome evaluate(TransactionContext ctx, RawFeatures f) {
        if (f.txnCount1m() >= blockThreshold) {
            return RuleOutcome.triggered("VELOCITY_1M", Decision.BLOCK,
                    f.txnCount1m() + " txns in last minute >= " + blockThreshold);
        }
        if (f.txnCount1m() >= reviewThreshold) {
            return RuleOutcome.triggered("VELOCITY_1M", Decision.REVIEW,
                    f.txnCount1m() + " txns in last minute >= " + reviewThreshold);
        }
        return RuleOutcome.notTriggered("VELOCITY_1M");
    }
}
