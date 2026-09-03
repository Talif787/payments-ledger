package com.fraud.application.service;

import com.fraud.application.decision.DecisionEngine;
import com.fraud.application.decision.DecisionResult;
import com.fraud.application.port.DecisionLog;
import com.fraud.application.port.FeatureStore;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.time.Clock;

/**
 * Orchestrates one evaluation: read the account's current features, decide, and
 * log the decision. Pure decision logic lives in DecisionEngine; this class only
 * wires the ports around it.
 */
public final class EvaluateService {

    private final FeatureStore featureStore;
    private final DecisionEngine decisionEngine;
    private final DecisionLog decisionLog;
    private final Clock clock;

    public EvaluateService(FeatureStore featureStore, DecisionEngine decisionEngine,
                           DecisionLog decisionLog, Clock clock) {
        this.featureStore = featureStore;
        this.decisionEngine = decisionEngine;
        this.decisionLog = decisionLog;
        this.clock = clock;
    }

    public DecisionResult evaluate(TransactionContext ctx) {
        RawFeatures raw = featureStore.computeRawFeatures(ctx.accountId(), ctx.counterpartyId(), ctx.at());
        DecisionResult result = decisionEngine.decide(ctx, raw);
        decisionLog.save(new DecisionRecord(clock.instant(), ctx, raw, result));
        return result;
    }
}
