package com.fraud.application.decision;

import com.fraud.application.feature.FeatureExtractor;
import com.fraud.application.feature.FeatureVector;
import com.fraud.application.model.LogisticRegressionModel;
import com.fraud.application.rule.RuleEngine;
import com.fraud.application.rule.RuleOutcome;
import com.fraud.domain.Decision;
import com.fraud.domain.DecisionMode;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines the rules and the model into one decision. In SHADOW mode the model is
 * scored and reported but only the rules enforce, so the effective decision never
 * depends on the model. In ACTIVE mode the effective decision is the stricter of
 * the rules and the model. Either way the model's decision and score are always
 * returned so they can be logged and evaluated.
 */
public final class DecisionEngine {

    private final RuleEngine ruleEngine;
    private final FeatureExtractor featureExtractor;
    private final LogisticRegressionModel model;
    private final DecisionMode mode;

    public DecisionEngine(RuleEngine ruleEngine, FeatureExtractor featureExtractor,
                          LogisticRegressionModel model, DecisionMode mode) {
        this.ruleEngine = ruleEngine;
        this.featureExtractor = featureExtractor;
        this.model = model;
        this.mode = mode;
    }

    public DecisionResult decide(TransactionContext ctx, RawFeatures raw) {
        RuleEngine.Result rules = ruleEngine.evaluate(ctx, raw);

        FeatureVector features = featureExtractor.extract(ctx, raw);
        double score = model.score(features);
        Decision modelDecision = model.classify(score);

        Decision effective = (mode == DecisionMode.ACTIVE)
                ? Decision.max(rules.decision(), modelDecision)
                : rules.decision();

        List<String> reasons = new ArrayList<>();
        for (RuleOutcome o : rules.triggered()) {
            reasons.add(o.ruleId() + ": " + o.reason());
        }
        if (mode == DecisionMode.ACTIVE && modelDecision != Decision.ALLOW) {
            reasons.add("MODEL: score " + String.format(java.util.Locale.ROOT, "%.3f", score));
        }

        return new DecisionResult(effective, rules.decision(), modelDecision, score, mode, reasons);
    }
}
