package com.fraud.application.rule;

import com.fraud.domain.Decision;

/** The result of evaluating one rule. severity is ALLOW when not triggered. */
public record RuleOutcome(String ruleId, boolean triggered, Decision severity, String reason) {

    public static RuleOutcome notTriggered(String ruleId) {
        return new RuleOutcome(ruleId, false, Decision.ALLOW, null);
    }

    public static RuleOutcome triggered(String ruleId, Decision severity, String reason) {
        return new RuleOutcome(ruleId, true, severity, reason);
    }
}
