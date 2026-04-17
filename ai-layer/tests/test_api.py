"""Integration tests for the FastAPI endpoints using httpx TestClient."""
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


# ── Health endpoint ────────────────────────────────────────────────────────

def test_health_returns_up():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"
    assert response.json()["service"] == "nexabank-ai-layer"


# ── Loan eligibility endpoint ──────────────────────────────────────────────

def test_loan_eligibility_eligible_profile():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "requested_amount": 15000,
        "term_months": 36,
        "annual_income": 80000,
        "credit_score": 780,
    })
    assert response.status_code == 200
    body = response.json()
    assert body["decision"] == "ELIGIBLE"
    assert "score" in body
    assert "dti" in body
    assert "loan_to_income" in body


def test_loan_eligibility_ineligible_profile():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "PERSONAL",
        "requested_amount": 10000,
        "term_months": 24,
        "annual_income": 40000,
        "credit_score": 510,
    })
    assert response.status_code == 200
    assert response.json()["decision"] == "INELIGIBLE"


def test_loan_eligibility_default_credit_score_used_when_omitted():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "requested_amount": 20000,
        "term_months": 60,
        "annual_income": 65000,
    })
    assert response.status_code == 200
    assert response.json()["decision"] in ("ELIGIBLE", "REVIEW", "INELIGIBLE")


def test_loan_eligibility_missing_required_field_returns_422():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "term_months": 60,
        "annual_income": 65000,
    })
    assert response.status_code == 422


def test_loan_eligibility_negative_amount_returns_422():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "requested_amount": -1000,
        "term_months": 60,
        "annual_income": 65000,
        "credit_score": 700,
    })
    assert response.status_code == 422


def test_loan_eligibility_credit_score_below_300_returns_422():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "requested_amount": 20000,
        "term_months": 60,
        "annual_income": 65000,
        "credit_score": 200,
    })
    assert response.status_code == 422


def test_loan_eligibility_credit_score_above_850_returns_422():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "AUTO",
        "requested_amount": 20000,
        "term_months": 60,
        "annual_income": 65000,
        "credit_score": 900,
    })
    assert response.status_code == 422


def test_loan_eligibility_response_contains_reason():
    response = client.post("/api/loan/eligibility", json={
        "loan_type": "MORTGAGE",
        "requested_amount": 300000,
        "term_months": 360,
        "annual_income": 120000,
        "credit_score": 760,
    })
    assert response.status_code == 200
    assert len(response.json()["reason"]) > 0


# ── Policy check endpoint ──────────────────────────────────────────────────

def test_policy_check_returns_in_review():
    response = client.post("/api/policy-check", json={"query": "overdraft fees"})
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "In Review"
    assert "overdraft fees" in body["reason"]


def test_policy_check_missing_query_returns_422():
    response = client.post("/api/policy-check", json={})
    assert response.status_code == 422


# ── OpenAPI docs ───────────────────────────────────────────────────────────

def test_swagger_ui_accessible():
    response = client.get("/docs")
    assert response.status_code == 200


def test_openapi_schema_accessible():
    response = client.get("/openapi.json")
    assert response.status_code == 200
    schema = response.json()
    assert "/api/loan/eligibility" in schema["paths"]
    assert "/api/policy-check" in schema["paths"]
