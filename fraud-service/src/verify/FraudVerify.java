package com.fraud.verify;

import com.fraud.application.WindowAggregator;
import com.fraud.application.decision.DecisionEngine;
import com.fraud.application.decision.DecisionResult;
import com.fraud.application.feature.FeatureExtractor;
import com.fraud.application.feature.FeatureVector;
import com.fraud.application.model.LogisticRegressionModel;
import com.fraud.application.rule.HardAmountCapRule;
import com.fraud.application.rule.NewCounterpartyRule;
import com.fraud.application.rule.RuleEngine;
import com.fraud.application.rule.SpendSpikeRule;
import com.fraud.application.rule.VelocityRule;
import com.fraud.domain.Activity;
import com.fraud.domain.Decision;
import com.fraud.domain.DecisionMode;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class FraudVerify {
    private static int passed = 0, failed = 0;
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    public static void main(String[] args) {
        windowAggregator();
        featureExtractor();
        rules();
        model();
        decisionEngineShadowVsActive();

        System.out.println();
        System.out.println("FRAUD RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void windowAggregator() {
        var agg = new WindowAggregator();
        List<Activity> recent = new ArrayList<>();
        recent.add(new Activity(NOW.minusSeconds(30), 1000, "cp1", "t1"));   // in 1m,5m,1h
        recent.add(new Activity(NOW.minusSeconds(120), 2000, "cp2", "t2"));  // in 5m,1h
        recent.add(new Activity(NOW.minusSeconds(1800), 3000, "cp1", "t3")); // in 1h
        recent.add(new Activity(NOW.minusSeconds(7200), 9999, "cp3", "t4")); // outside 1h

        RawFeatures f = agg.compute(recent, "cpNew", NOW);
        check("count1m counts only last minute", f.txnCount1m() == 1);
        check("count5m counts last 5 minutes", f.txnCount5m() == 2);
        check("count1h counts last hour", f.txnCount1h() == 3);
        check("sum1h sums last hour only", f.amountSum1hMinor() == 6000);
        check("distinct counterparties in 1h", f.distinctCounterparties1h() == 2); // cp1, cp2
        check("seconds since last is nearest", f.secondsSinceLastTxn() == 30);
        check("proposed counterparty is new", f.newCounterparty());

        RawFeatures f2 = agg.compute(recent, "cp1", NOW);
        check("known counterparty is not new", !f2.newCounterparty());

        RawFeatures f3 = agg.compute(List.of(), "cpX", NOW);
        check("no activity => secondsSinceLast is MAX", f3.secondsSinceLastTxn() == Long.MAX_VALUE);
    }

    private static void featureExtractor() {
        var ex = new FeatureExtractor();
        var ctx = new TransactionContext("acc", 10000, "USD", "cp", NOW);
        var raw = new RawFeatures(2, 3, 5, 50000, 4, 30, true);
        FeatureVector v = ex.extract(ctx, raw);
        check("amount is log-scaled", Math.abs(v.amountLog() - Math.log1p(100.0)) < 1e-9);
        check("newCounterparty maps to 1.0", v.newCounterparty() == 1.0);
        check("recency high when recent", v.recencyScore() > 0.8);
        var rawNone = new RawFeatures(0, 0, 0, 0, 0, Long.MAX_VALUE, false);
        check("recency 0 when no prior activity",
                ex.extract(ctx, rawNone).recencyScore() == 0.0);
    }

    private static RuleEngine defaultEngine() {
        return new RuleEngine(List.of(
                new HardAmountCapRule(1_000_000),       // 10,000.00
                new VelocityRule(5, 10),
                new NewCounterpartyRule(50_000),        // 500.00
                new SpendSpikeRule(2_000_000)));        // 20,000.00
    }

    private static void rules() {
        var engine = defaultEngine();
        var quiet = new RawFeatures(1, 1, 1, 1000, 1, 120, false);

        var normal = engine.evaluate(new TransactionContext("a", 5000, "USD", "cpKnown", NOW), quiet);
        check("normal transaction passes rules", normal.decision() == Decision.ALLOW);

        var big = engine.evaluate(new TransactionContext("a", 1_000_000, "USD", "cpKnown", NOW), quiet);
        check("hard cap blocks", big.decision() == Decision.BLOCK);

        var fast = new RawFeatures(10, 12, 20, 5000, 3, 5, false);
        var burst = engine.evaluate(new TransactionContext("a", 5000, "USD", "cpKnown", NOW), fast);
        check("velocity block at 10/min", burst.decision() == Decision.BLOCK);

        var newcp = engine.evaluate(new TransactionContext("a", 60000, "USD", "cpNew", NOW),
                new RawFeatures(1, 1, 1, 1000, 1, 120, true));
        check("new counterparty large => review", newcp.decision() == Decision.REVIEW);
    }

    private static void model() {
        var model = LogisticRegressionModel.illustrativeDefault();
        var ex = new FeatureExtractor();

        var calm = ex.extract(new TransactionContext("a", 2000, "USD", "cp", NOW),
                new RawFeatures(0, 0, 1, 2000, 1, 3600, false));
        double calmScore = model.score(calm);
        check("calm score is low", calmScore < 0.2);
        check("calm classifies ALLOW", model.classify(calmScore) == Decision.ALLOW);

        var risky = ex.extract(new TransactionContext("a", 500000, "USD", "cpNew", NOW),
                new RawFeatures(8, 10, 15, 800000, 6, 5, true));
        double riskyScore = model.score(risky);
        check("risky score higher than calm", riskyScore > calmScore);
        check("score is a probability in [0,1]", riskyScore >= 0.0 && riskyScore <= 1.0);
    }

    private static void decisionEngineShadowVsActive() {
        var ex = new FeatureExtractor();
        var rules = defaultEngine();
        // Force the model to BLOCK by using an always-high model (bias very high).
        var alwaysBlock = new LogisticRegressionModel(new double[8], 100.0, 0.5, 0.85);
        var ctx = new TransactionContext("a", 5000, "USD", "cpKnown", NOW);
        var quiet = new RawFeatures(1, 1, 1, 1000, 1, 120, false); // rules say ALLOW

        var shadow = new DecisionEngine(rules, ex, alwaysBlock, DecisionMode.SHADOW);
        DecisionResult s = shadow.decide(ctx, quiet);
        check("shadow: model does not enforce (effective ALLOW)", s.effectiveDecision() == Decision.ALLOW);
        check("shadow: model decision still reported (BLOCK)", s.modelDecision() == Decision.BLOCK);
        check("shadow: rules decision reported (ALLOW)", s.rulesDecision() == Decision.ALLOW);

        var active = new DecisionEngine(rules, ex, alwaysBlock, DecisionMode.ACTIVE);
        DecisionResult a = active.decide(ctx, quiet);
        check("active: model can enforce (effective BLOCK)", a.effectiveDecision() == Decision.BLOCK);

        // In active mode, rules still enforce even if model says allow.
        var alwaysAllow = new LogisticRegressionModel(new double[8], -100.0, 0.5, 0.85);
        var activeRulesBlock = new DecisionEngine(rules, ex, alwaysAllow, DecisionMode.ACTIVE);
        DecisionResult r = activeRulesBlock.decide(
                new TransactionContext("a", 1_000_000, "USD", "cp", NOW), quiet);
        check("active: rules still block when model allows", r.effectiveDecision() == Decision.BLOCK);
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
}
