package com.fraud.application;

import com.fraud.domain.Activity;
import com.fraud.domain.RawFeatures;
import java.time.Instant;
import java.util.List;

/**
 * Pure reference computation of streaming features from an account's recent
 * activity. The Redis feature store is the production implementation; this class
 * defines the exact semantics both share and is what the offline tests verify.
 */
public final class WindowAggregator {

    public RawFeatures compute(List<Activity> recent, String proposedCounterparty, Instant now) {
        int c1m = 0, c5m = 0, c1h = 0, distinct1h = 0;
        long sum1h = 0L;
        long secondsSinceLast = Long.MAX_VALUE;
        boolean counterpartySeen = false;
        java.util.Set<String> counterparties1h = new java.util.HashSet<>();

        for (Activity a : recent) {
            long ageSec = (now.toEpochMilli() - a.at().toEpochMilli()) / 1000L;
            if (ageSec < 0) {
                continue; // ignore activity in the future relative to now
            }
            if (ageSec <= 60) c1m++;
            if (ageSec <= 300) c5m++;
            if (ageSec <= 3600) {
                c1h++;
                sum1h += a.amountMinor();
                if (a.counterpartyId() != null) counterparties1h.add(a.counterpartyId());
            }
            secondsSinceLast = Math.min(secondsSinceLast, ageSec);
            if (proposedCounterparty != null && proposedCounterparty.equals(a.counterpartyId())) {
                counterpartySeen = true;
            }
        }
        distinct1h = counterparties1h.size();
        boolean newCounterparty = proposedCounterparty != null && !counterpartySeen;

        return new RawFeatures(c1m, c5m, c1h, sum1h, distinct1h, secondsSinceLast, newCounterparty);
    }
}
