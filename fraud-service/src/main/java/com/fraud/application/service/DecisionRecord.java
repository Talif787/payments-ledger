package com.fraud.application.service;

import com.fraud.application.decision.DecisionResult;
import com.fraud.domain.RawFeatures;
import com.fraud.domain.TransactionContext;
import java.time.Instant;

/** One evaluation, context plus features plus outcome, as written to the log. */
public record DecisionRecord(
        Instant at,
        TransactionContext context,
        RawFeatures features,
        DecisionResult result) {
}
