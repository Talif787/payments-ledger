package com.fraud.domain;

/**
 * Whether the model has authority. In SHADOW the model is scored and logged but
 * only the rules enforce; in ACTIVE the model can also block or flag. Shadow mode
 * lets a model be evaluated against real outcomes before it is given authority.
 */
public enum DecisionMode {
    SHADOW,
    ACTIVE
}
