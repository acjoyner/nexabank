package com.nexabank.loan.strategy;

import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.dto.EligibilityResult;

/**
 * Strategy Pattern — Loan Eligibility.
 *
 * The Strategy pattern defines a family of algorithms, encapsulates each one,
 * and makes them interchangeable. Here we have two strategies:
 *
 * 1. AiEligibilityStrategy — delegates to Python FastAPI AI layer
 * 2. RuleBasedEligibilityStrategy — deterministic rules (fallback when AI is unavailable)
 *
 * The LoanApplicationService selects the strategy at runtime:
 * - Default: AiEligibilityStrategy (AI-assisted decision)
 * - Fallback: RuleBasedEligibilityStrategy (if AI layer is down)
 *
 * This pattern makes it easy to add new strategies (e.g., third-party credit bureau)
 * without changing the service code.
 *
 * See docs/learning/11-design-patterns-used.md
 */
public interface LoanEligibilityStrategy {

    EligibilityResult evaluate(LoanApplicationRequest request);
}
