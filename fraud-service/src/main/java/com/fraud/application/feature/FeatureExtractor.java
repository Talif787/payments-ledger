package com.fraud.application.feature;

import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/**
 * Turns raw streaming features and the proposed transaction into the model's
 * normalized feature vector. Amounts are log-scaled (in major units) so a few
 * large values do not dominate; recency is mapped to a 0..1 score that is high
 * when the previous transaction was very recent (a burst signal).
 */
public final class FeatureExtractor {

    public FeatureVector extract(TransactionContext ctx, RawFeatures f) {
        double amountLog = Math.log1p(ctx.amountMinor() / 100.0);
        double sumLog = Math.log1p(f.amountSum1hMinor() / 100.0);
        double recencyScore = recency(f.secondsSinceLastTxn());
        return new FeatureVector(
                amountLog,
                f.txnCount1m(),
                f.txnCount5m(),
                f.txnCount1h(),
                sumLog,
                f.distinctCounterparties1h(),
                f.newCounterparty() ? 1.0 : 0.0,
                recencyScore);
    }

    /** 1.0 when the last transaction was just now, decaying to 0 over ~5 minutes. */
    private double recency(long secondsSinceLast) {
        if (secondsSinceLast == Long.MAX_VALUE) {
            return 0.0;
        }
        double halfLifeSeconds = 300.0;
        return Math.exp(-((double) secondsSinceLast) / halfLifeSeconds);
    }
}
