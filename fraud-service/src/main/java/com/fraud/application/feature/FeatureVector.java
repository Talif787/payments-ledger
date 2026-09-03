package com.fraud.application.feature;

/**
 * Ordered, normalized features fed to the model. Field order here is the model's
 * input contract and must match the model's weight order.
 */
public record FeatureVector(
        double amountLog,
        double count1m,
        double count5m,
        double count1h,
        double amountSum1hLog,
        double distinctCounterparties1h,
        double newCounterparty,
        double recencyScore) {

    public double[] toArray() {
        return new double[] {
                amountLog, count1m, count5m, count1h,
                amountSum1hLog, distinctCounterparties1h, newCounterparty, recencyScore
        };
    }

    public static String[] names() {
        return new String[] {
                "amountLog", "count1m", "count5m", "count1h",
                "amountSum1hLog", "distinctCounterparties1h", "newCounterparty", "recencyScore"
        };
    }
}
