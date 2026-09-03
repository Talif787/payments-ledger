package com.fraud.infrastructure.config;

import com.fraud.application.WindowAggregator;
import com.fraud.application.decision.DecisionEngine;
import com.fraud.application.feature.FeatureExtractor;
import com.fraud.application.model.LogisticRegressionModel;
import com.fraud.application.port.DecisionLog;
import com.fraud.application.port.FeatureStore;
import com.fraud.application.rule.HardAmountCapRule;
import com.fraud.application.rule.NewCounterpartyRule;
import com.fraud.application.rule.Rule;
import com.fraud.application.rule.RuleEngine;
import com.fraud.application.rule.SpendSpikeRule;
import com.fraud.application.rule.VelocityRule;
import com.fraud.application.service.EvaluateService;
import com.fraud.domain.DecisionMode;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pure fraud components with thresholds and mode from configuration.
 * The mode (SHADOW or ACTIVE) is read here, so shipping the model into
 * enforcement is a config change, not a code change.
 */
@Configuration
public class BeansConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public WindowAggregator windowAggregator() {
        return new WindowAggregator();
    }

    @Bean
    public FeatureExtractor featureExtractor() {
        return new FeatureExtractor();
    }

    @Bean
    public LogisticRegressionModel model() {
        return LogisticRegressionModel.illustrativeDefault();
    }

    @Bean
    public RuleEngine ruleEngine(
            @Value("${fraud.rules.hard-amount-cap-minor:1000000}") long hardCap,
            @Value("${fraud.rules.velocity-review:5}") int velReview,
            @Value("${fraud.rules.velocity-block:10}") int velBlock,
            @Value("${fraud.rules.new-counterparty-amount-minor:50000}") long newCpAmount,
            @Value("${fraud.rules.hourly-spend-cap-minor:2000000}") long spendCap) {
        List<Rule> rules = List.of(
                new HardAmountCapRule(hardCap),
                new VelocityRule(velReview, velBlock),
                new NewCounterpartyRule(newCpAmount),
                new SpendSpikeRule(spendCap));
        return new RuleEngine(rules);
    }

    @Bean
    public DecisionEngine decisionEngine(RuleEngine ruleEngine, FeatureExtractor featureExtractor,
                                         LogisticRegressionModel model,
                                         @Value("${fraud.mode:SHADOW}") String mode) {
        return new DecisionEngine(ruleEngine, featureExtractor, model,
                DecisionMode.valueOf(mode.toUpperCase()));
    }

    @Bean
    public EvaluateService evaluateService(FeatureStore featureStore, DecisionEngine decisionEngine,
                                           DecisionLog decisionLog, Clock clock) {
        return new EvaluateService(featureStore, decisionEngine, decisionLog, clock);
    }
}
