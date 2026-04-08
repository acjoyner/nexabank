package com.nexabank.loan.strategy;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rule-Based Eligibility Strategy — deterministic fallback.
 *
 * Used when AI layer is unavailable (circuit breaker open).
 * Rules mirror standard banking underwriting criteria:
 *
 * 1. Debt-to-Income ratio (DTI): loan payment / annual income < 0.43 (FHA guideline)
 * 2. Credit score: >= 620 for approval, 580-619 for review, < 580 rejected
 * 3. Loan-to-Income ratio: requested amount < 5x annual income
 *
 * See docs/learning/11-design-patterns-used.md (Strategy section)
 */
@Component("ruleBasedStrategy")
@Slf4j
public class RuleBasedEligibilityStrategy implements LoanEligibilityStrategy {

    @Override
    public EligibilityResult evaluate(LoanApplicationRequest request) {
        log.info("Evaluating loan eligibility using rule-based strategy");

        // Monthly loan payment estimate (simple: amount / months)
        BigDecimal monthlyPayment = request.getRequestedAmount()
                .divide(BigDecimal.valueOf(request.getTermMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyIncome = request.getAnnualIncome()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal dti = monthlyPayment.divide(monthlyIncome, 4, RoundingMode.HALF_UP);

        // Loan-to-Income check
        BigDecimal loanToIncome = request.getRequestedAmount()
                .divide(request.getAnnualIncome(), 4, RoundingMode.HALF_UP);

        int creditScore = request.getCreditScore() != null ? request.getCreditScore() : 0;

        if (dti.compareTo(new BigDecimal("0.43")) > 0) {
            return EligibilityResult.builder()
                    .decision("INELIGIBLE")
                    .reason(String.format("DTI ratio %.2f%% exceeds 43%% limit. " +
                            "Consider a smaller loan or longer term.", dti.multiply(BigDecimal.valueOf(100))))
                    .strategyUsed("RULE_BASED")
                    .build();
        }

        if (loanToIncome.compareTo(new BigDecimal("5.0")) > 0) {
            return EligibilityResult.builder()
                    .decision("INELIGIBLE")
                    .reason("Requested amount exceeds 5x annual income limit.")
                    .strategyUsed("RULE_BASED")
                    .build();
        }

        if (creditScore < 580) {
            return EligibilityResult.builder()
                    .decision("INELIGIBLE")
                    .reason("Credit score " + creditScore + " is below minimum threshold of 580.")
                    .strategyUsed("RULE_BASED")
                    .build();
        }

        if (creditScore < 620) {
            return EligibilityResult.builder()
                    .decision("REVIEW")
                    .reason("Credit score " + creditScore + " qualifies for manual review (580-619 range).")
                    .strategyUsed("RULE_BASED")
                    .build();
        }

        return EligibilityResult.builder()
                .decision("ELIGIBLE")
                .reason(String.format("Approved: DTI %.1f%%, credit score %d, loan-to-income %.1fx",
                        dti.multiply(BigDecimal.valueOf(100)), creditScore, loanToIncome))
                .strategyUsed("RULE_BASED")
                .build();
    }
}
