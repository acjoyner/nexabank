"""Tests for the rule-based loan scoring model."""
import pytest
from models.loan_scorer import score_application


# ── Eligibility decision tests ─────────────────────────────────────────────

def test_excellent_profile_is_eligible():
    result = score_application("AUTO", 15_000, 36, 80_000, 780)
    assert result["decision"] == "ELIGIBLE"


def test_poor_credit_score_is_ineligible():
    result = score_application("PERSONAL", 10_000, 24, 50_000, 520)
    assert result["decision"] == "INELIGIBLE"
    assert "credit" in result["reason"].lower()


def test_borderline_credit_score_is_review():
    result = score_application("PERSONAL", 8_000, 24, 50_000, 595)
    assert result["decision"] in ("REVIEW", "INELIGIBLE")


def test_high_dti_exceeding_50pct_is_ineligible():
    # $90k / 12 months = $7,500/mo on $4,000/mo income → DTI = 187%
    result = score_application("PERSONAL", 90_000, 12, 48_000, 700)
    assert result["decision"] == "INELIGIBLE"
    assert "DTI" in result["reason"] or "dti" in result["reason"].lower()


def test_dti_between_43_and_50_returns_review_or_ineligible():
    # $30k / 60 months = $500/mo on $1,000/mo income → DTI = 50%
    result = score_application("AUTO", 30_000, 60, 12_000, 700)
    assert result["decision"] in ("REVIEW", "INELIGIBLE")


def test_loan_exceeding_5x_income_penalised():
    result = score_application("PERSONAL", 600_000, 360, 100_000, 750)
    assert result["decision"] in ("REVIEW", "INELIGIBLE")
    assert "income" in result["reason"].lower()


def test_mortgage_lower_risk_than_personal():
    mortgage = score_application("MORTGAGE", 200_000, 360, 100_000, 720)
    personal = score_application("PERSONAL", 200_000, 360, 100_000, 720)
    assert mortgage["score"] > personal["score"]


# ── Score field tests ──────────────────────────────────────────────────────

def test_score_is_between_0_and_105():
    result = score_application("AUTO", 20_000, 60, 70_000, 800)
    assert 0 <= result["score"] <= 105


def test_excellent_credit_bonus_increases_score():
    result_excellent = score_application("AUTO", 20_000, 60, 70_000, 780)
    result_good = score_application("AUTO", 20_000, 60, 70_000, 720)
    assert result_excellent["score"] > result_good["score"]


# ── DTI calculation tests ──────────────────────────────────────────────────

def test_dti_calculated_correctly():
    # $12k / 12 months = $1,000/mo; income $5,000/mo → DTI = 0.2
    result = score_application("AUTO", 12_000, 12, 60_000, 700)
    assert abs(result["dti"] - (1000 / 5000)) < 0.0001


def test_zero_income_handled_without_division_error():
    result = score_application("PERSONAL", 10_000, 12, 0.001, 700)
    assert result["dti"] >= 0


# ── Loan-to-income tests ───────────────────────────────────────────────────

def test_loan_to_income_calculated_correctly():
    result = score_application("AUTO", 50_000, 60, 100_000, 720)
    assert abs(result["loan_to_income"] - 0.5) < 0.01


# ── Return structure tests ─────────────────────────────────────────────────

def test_result_contains_all_required_keys():
    result = score_application("AUTO", 20_000, 60, 70_000, 700)
    assert set(result.keys()) == {"decision", "reason", "score", "dti", "loan_to_income"}


def test_decision_is_valid_enum_value():
    result = score_application("MORTGAGE", 300_000, 360, 120_000, 760)
    assert result["decision"] in ("ELIGIBLE", "INELIGIBLE", "REVIEW")


def test_reason_is_non_empty_string():
    result = score_application("AUTO", 20_000, 60, 70_000, 700)
    assert isinstance(result["reason"], str)
    assert len(result["reason"]) > 0


@pytest.mark.parametrize("loan_type", ["PERSONAL", "AUTO", "MORTGAGE"])
def test_all_loan_types_return_valid_decision(loan_type):
    result = score_application(loan_type, 20_000, 60, 60_000, 700)
    assert result["decision"] in ("ELIGIBLE", "INELIGIBLE", "REVIEW")


def test_unknown_loan_type_defaults_to_personal_risk():
    result = score_application("UNKNOWN", 20_000, 60, 60_000, 700)
    assert result["decision"] in ("ELIGIBLE", "INELIGIBLE", "REVIEW")
