package com.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraud.application.decision.DecisionEngine;
import com.fraud.application.decision.DecisionResult;
import com.fraud.application.feature.FeatureExtractor;
import com.fraud.application.model.LogisticRegressionModel;
import com.fraud.application.rule.HardAmountCapRule;
import com.fraud.application.rule.NewCounterpartyRule;
import com.fraud.application.rule.RuleEngine;
import com.fraud.application.rule.SpendSpikeRule;
import com.fraud.application.rule.VelocityRule;
import com.fraud.domain.Decision;
import com.fraud.domain.DecisionMode;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecisionEngineShadowTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private final FeatureExtractor extractor = new FeatureExtractor();
    private final RuleEngine rules = new RuleEngine(List.of(
            new HardAmountCapRule(1_000_000),
            new VelocityRule(5, 10),
            new NewCounterpartyRule(50_000),
            new SpendSpikeRule(2_000_000)));

    private final RawFeatures quiet = new RawFeatures(1, 1, 1, 1000, 1, 120, false);
    private final TransactionContext smallKnown =
            new TransactionContext("a", 5000, "USD", "cpKnown", NOW);

    @Test
    void shadowModeModelDoesNotEnforceButIsReported() {
        var alwaysBlockModel = new LogisticRegressionModel(new double[8], 100.0, 0.5, 0.85);
        var engine = new DecisionEngine(rules, extractor, alwaysBlockModel, DecisionMode.SHADOW);

        DecisionResult r = engine.decide(smallKnown, quiet);

        assertThat(r.effectiveDecision()).isEqualTo(Decision.ALLOW);
        assertThat(r.modelDecision()).isEqualTo(Decision.BLOCK);
        assertThat(r.rulesDecision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void activeModeModelCanEnforce() {
        var alwaysBlockModel = new LogisticRegressionModel(new double[8], 100.0, 0.5, 0.85);
        var engine = new DecisionEngine(rules, extractor, alwaysBlockModel, DecisionMode.ACTIVE);

        assertThat(engine.decide(smallKnown, quiet).effectiveDecision()).isEqualTo(Decision.BLOCK);
    }

    @Test
    void rulesEnforceEvenWhenModelAllows() {
        var alwaysAllowModel = new LogisticRegressionModel(new double[8], -100.0, 0.5, 0.85);
        var engine = new DecisionEngine(rules, extractor, alwaysAllowModel, DecisionMode.ACTIVE);
        var overCap = new TransactionContext("a", 1_000_000, "USD", "cp", NOW);

        assertThat(engine.decide(overCap, quiet).effectiveDecision()).isEqualTo(Decision.BLOCK);
    }
}
