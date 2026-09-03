package com.fraud.application.port;

import com.fraud.domain.Activity;
import com.fraud.domain.RawFeatures;
import java.time.Instant;

/**
 * Stores and serves per-account streaming features. Backed by Redis in
 * production. recordActivity is idempotent on the transaction id so at-least-once
 * stream delivery does not inflate the features.
 */
public interface FeatureStore {
    void recordActivity(String accountId, Activity activity);

    RawFeatures computeRawFeatures(String accountId, String proposedCounterparty, Instant now);
}
