package com.fraud.application.model;

import com.fraud.application.feature.FeatureVector;
import com.fraud.domain.Decision;

/**
 * A logistic regression scorer. The serving path is real: score = sigmoid(bias +
 * weights . features), then thresholds map the probability to a decision. The
 * weights are illustrative placeholders chosen to be directionally sensible; in
 * production they come from an offline-trained model and are loaded, not
 * hard-coded. Weight order must match FeatureVector.toArray().
 */
public final class LogisticRegressionModel {

    private final double[] weights;
    private final double bias;
    private final double reviewThreshold;
    private final double blockThreshold;

    public LogisticRegressionModel(double[] weights, double bias,
                                   double reviewThreshold, double blockThreshold) {
        this.weights = weights.clone();
        this.bias = bias;
        this.reviewThreshold = reviewThreshold;
        this.blockThreshold = blockThreshold;
    }

    /** A directionally sensible default: velocity, new counterparty, and size raise risk. */
    public static LogisticRegressionModel illustrativeDefault() {
        // order: amountLog, count1m, count5m, count1h, amountSum1hLog,
        //        distinctCounterparties1h, newCounterparty, recencyScore
        double[] w = { 0.35, 0.80, 0.30, 0.10, 0.25, 0.15, 0.90, 0.70 };
        return new LogisticRegressionModel(w, -4.0, 0.5, 0.85);
    }

    public double score(FeatureVector features) {
        double[] x = features.toArray();
        if (x.length != weights.length) {
            throw new IllegalArgumentException(
                    "feature/weight length mismatch: " + x.length + " vs " + weights.length);
        }
        double z = bias;
        for (int i = 0; i < x.length; i++) {
            z += weights[i] * x[i];
        }
        return sigmoid(z);
    }

    public Decision classify(double score) {
        if (score >= blockThreshold) return Decision.BLOCK;
        if (score >= reviewThreshold) return Decision.REVIEW;
        return Decision.ALLOW;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
