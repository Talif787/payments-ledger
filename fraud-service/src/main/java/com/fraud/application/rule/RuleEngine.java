package com.fraud.application.rule;

import com.fraud.domain.Decision;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.util.ArrayList;
import java.util.List;

/** Runs all rules and reports the triggered outcomes and their combined severity. */
public final class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Result evaluate(TransactionContext ctx, RawFeatures features) {
        List<RuleOutcome> triggered = new ArrayList<>();
        Decision decision = Decision.ALLOW;
        for (Rule rule : rules) {
            RuleOutcome outcome = rule.evaluate(ctx, features);
            if (outcome.triggered()) {
                triggered.add(outcome);
                decision = Decision.max(decision, outcome.severity());
            }
        }
        return new Result(decision, List.copyOf(triggered));
    }

    public record Result(Decision decision, List<RuleOutcome> triggered) {}
}
