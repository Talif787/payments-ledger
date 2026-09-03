package com.fraud.application.rule;

import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;

/** A deterministic fraud rule. Pure: same inputs always yield the same outcome. */
public interface Rule {
    RuleOutcome evaluate(TransactionContext ctx, RawFeatures features);
}
