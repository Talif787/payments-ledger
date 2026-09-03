package com.fraud.presentation.web.dto;

import java.util.List;

/** The screening outcome. decision is what the caller should enforce. */
public record EvaluateResponse(
        String decision,
        String rulesDecision,
        String modelDecision,
        double modelScore,
        String mode,
        List<String> reasons) {
}
