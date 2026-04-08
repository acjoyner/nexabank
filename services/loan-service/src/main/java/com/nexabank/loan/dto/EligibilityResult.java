package com.nexabank.loan.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EligibilityResult {
    private String decision;   // ELIGIBLE, INELIGIBLE, REVIEW
    private String reason;
    private String strategyUsed;  // AI or RULE_BASED
}
