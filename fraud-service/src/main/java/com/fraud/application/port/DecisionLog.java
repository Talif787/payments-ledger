package com.fraud.application.port;

import com.fraud.application.service.DecisionRecord;

/** Durable log of every evaluation, for evaluating the model against outcomes. */
public interface DecisionLog {
    void save(DecisionRecord record);
}
