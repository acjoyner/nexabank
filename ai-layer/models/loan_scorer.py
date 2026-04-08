"""
NexaBank Loan Eligibility Scorer — Rule-based mock ML model.

In production this would be a trained scikit-learn or XGBoost model.
For this demo, uses deterministic rules that produce realistic outputs
without requiring training data or model files.

Scoring factors (mirrors FICO/FHA underwriting criteria):
- Credit score: weighted 40%
- Debt-to-Income (DTI): weighted 35%
- Loan-to-Income ratio: weighted 15%
- Loan type risk: weighted 10%
"""

from decimal import Decimal


LOAN_TYPE_RISK = {
    "PERSONAL": 0.15,    # Higher risk — no collateral
    "AUTO": 0.08,        # Medium risk — vehicle as collateral
    "MORTGAGE": 0.05,    # Lower risk — property as collateral
}


def score_application(
    loan_type: str,
    requested_amount: float,
    term_months: int,
    annual_income: float,
    credit_score: int,
) -> dict:
    """
    Score a loan application and return a decision with reason.

    Returns: { decision: "ELIGIBLE"|"INELIGIBLE"|"REVIEW", reason: str, score: float }
    """
    issues = []
    score = 100.0  # Start at 100, deduct for risk factors

    # 1. Credit score assessment (40% weight)
    if credit_score < 580:
        score -= 40
        issues.append(f"credit score {credit_score} below minimum threshold (580)")
    elif credit_score < 620:
        score -= 20
        issues.append(f"credit score {credit_score} in review range (580-619)")
    elif credit_score < 700:
        score -= 5
    elif credit_score >= 750:
        score += 5  # Excellent credit bonus

    # 2. Debt-to-Income ratio (35% weight)
    monthly_income = annual_income / 12
    monthly_payment = requested_amount / term_months
    dti = monthly_payment / monthly_income if monthly_income > 0 else 1.0

    if dti > 0.50:
        score -= 35
        issues.append(f"DTI {dti:.1%} exceeds 50% maximum")
    elif dti > 0.43:
        score -= 20
        issues.append(f"DTI {dti:.1%} exceeds FHA 43% guideline")
    elif dti > 0.36:
        score -= 8
    # DTI < 28% is excellent — no deduction

    # 3. Loan-to-Income ratio (15% weight)
    loan_to_income = requested_amount / annual_income if annual_income > 0 else 10
    if loan_to_income > 5:
        score -= 15
        issues.append(f"loan amount is {loan_to_income:.1f}x annual income (max 5x)")
    elif loan_to_income > 3:
        score -= 5

    # 4. Loan type risk (10% weight)
    type_risk = LOAN_TYPE_RISK.get(loan_type, 0.15)
    score -= type_risk * 10

    # Decision thresholds
    if score >= 75:
        decision = "ELIGIBLE"
        reason = (
            f"Strong application: credit score {credit_score}, "
            f"DTI {dti:.1%}, {loan_type.lower()} loan approved. "
            f"AI confidence score: {score:.0f}/100."
        )
    elif score >= 55:
        decision = "REVIEW"
        reason = (
            f"Manual review recommended. Factors: {'; '.join(issues) if issues else 'borderline metrics'}. "
            f"AI confidence score: {score:.0f}/100."
        )
    else:
        decision = "INELIGIBLE"
        reason = (
            f"Application declined. Issues: {'; '.join(issues)}. "
            f"Consider improving credit score or reducing loan amount. "
            f"AI confidence score: {score:.0f}/100."
        )

    return {
        "decision": decision,
        "reason": reason,
        "score": round(score, 1),
        "dti": round(dti, 4),
        "loan_to_income": round(loan_to_income, 2),
    }
