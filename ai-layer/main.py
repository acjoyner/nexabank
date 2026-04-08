"""
NexaBank AI Layer — FastAPI application

Endpoints:
  POST /api/loan/eligibility   — loan eligibility scoring (called by loan-service)
  POST /api/policy-check       — legacy policy check (original AILayer endpoint)
  GET  /health                 — health check

Architecture:
  Spring Boot loan-service → (HTTP POST) → FastAPI ai-layer → loan_scorer.py
  This is the Python sidecar pattern — a lightweight AI/ML service called by Java.

See docs/learning/01-spring-boot-microservices.md (Strangler Fig / Sidecar pattern)
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import logging

from models.loan_scorer import score_application

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="NexaBank AI Layer",
    description="AI-assisted loan eligibility scoring and policy checks",
    version="1.0.0"
)

# Allow Spring Boot services and Angular frontend to call this
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─────────────────────────────────────────────────────────────
# Request / Response models (Pydantic validation)
# ─────────────────────────────────────────────────────────────

class LoanEligibilityRequest(BaseModel):
    loan_type: str = Field(..., description="PERSONAL, AUTO, or MORTGAGE")
    requested_amount: float = Field(..., gt=0)
    term_months: int = Field(..., gt=0)
    annual_income: float = Field(..., gt=0)
    credit_score: int = Field(default=650, ge=300, le=850)


class LoanEligibilityResponse(BaseModel):
    decision: str           # ELIGIBLE, INELIGIBLE, REVIEW
    reason: str
    score: float
    dti: float
    loan_to_income: float


class PolicyCheckRequest(BaseModel):
    query: str


class PolicyCheckResponse(BaseModel):
    status: str
    reason: str


# ─────────────────────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────────────────────

@app.post("/api/loan/eligibility", response_model=LoanEligibilityResponse)
def loan_eligibility(request: LoanEligibilityRequest) -> LoanEligibilityResponse:
    """
    AI-powered loan eligibility scoring.
    Called by loan-service's AiEligibilityStrategy via RestClient.
    """
    logger.info(
        "Scoring loan: type=%s amount=%.2f income=%.2f credit=%d",
        request.loan_type, request.requested_amount,
        request.annual_income, request.credit_score
    )

    result = score_application(
        loan_type=request.loan_type,
        requested_amount=request.requested_amount,
        term_months=request.term_months,
        annual_income=request.annual_income,
        credit_score=request.credit_score,
    )

    logger.info("AI decision: %s (score: %.1f)", result["decision"], result["score"])
    return LoanEligibilityResponse(**result)


@app.post("/api/policy-check", response_model=PolicyCheckResponse)
def policy_check(request: PolicyCheckRequest) -> PolicyCheckResponse:
    """
    Legacy policy check endpoint — from original SpringBoot-Java-Angular-Python project.
    Kept for backward compatibility.

    CONCEPT: RAG (Retrieval Augmented Generation) simulation.
    In a production system:
    1. embedding = model.encode(request.query)
    2. docs = vector_db.search(embedding, filter={'policy_version': 'v4.2'})
    3. response = llm.generate(docs + query)
    """
    return PolicyCheckResponse(
        status="In Review",
        reason=(
            f"Retrieved policy docs for '{request.query}'. "
            f"Eligibility depends on credit-to-debt ratio > 0.4 "
            f"per Bank Policy v4.2."
        )
    )


@app.get("/health")
def health():
    return {"status": "UP", "service": "nexabank-ai-layer"}
