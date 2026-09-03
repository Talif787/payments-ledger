package com.fraud.application.decision;

import com.fraud.domain.Decision;
import com.fraud.domain.DecisionMode;
import java.util.List;

/**
 * The full outcome of an evaluation. effectiveDecision is what is enforced;
 * rulesDecision and modelDecision are the independent contributions, and mode
 * records whether the model had authority. Logging all of them is what makes
 * shadow-mode evaluation possible.
 */
public record DecisionResult(
        Decision effectiveDecision,
        Decision rulesDecision,
        Decision modelDecision,
        double modelScore,
        DecisionMode mode,
        List<String> reasons) {
}
