package com.nexabank.loan.service;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.dto.LoanApplicationResponse;
import com.nexabank.loan.event.LoanStatusChangedEvent;
import com.nexabank.loan.model.*;
import com.nexabank.loan.repository.LoanApplicationRepository;
import com.nexabank.loan.strategy.AiEligibilityStrategy;
import com.nexabank.loan.strategy.LoanEligibilityStrategy;
import com.nexabank.loan.strategy.RuleBasedEligibilityStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

/**
 * Loan Application Service — orchestrates the loan workflow.
 *
 * Strategy selection:
 * 1. Try AiEligibilityStrategy first (AI-powered decision)
 * 2. If AI layer is unavailable (RestClientException), fall back to
 *    RuleBasedEligibilityStrategy (deterministic rules)
 *
 * This is the Strategy Pattern in action — the service delegates
 * the eligibility algorithm to the selected strategy without
 * knowing which implementation is actually running.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private final LoanApplicationRepository loanRepository;
    private final AiEligibilityStrategy aiStrategy;
    private final RuleBasedEligibilityStrategy ruleStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public LoanApplicationResponse apply(LoanApplicationRequest request) {
        // Evaluate eligibility using AI (with rule-based fallback)
        EligibilityResult eligibility = evaluate(request);

        // Map decision to loan status
        LoanStatus status = switch (eligibility.getDecision()) {
            case "ELIGIBLE"   -> LoanStatus.APPROVED;
            case "INELIGIBLE" -> LoanStatus.REJECTED;
            default           -> LoanStatus.AI_REVIEW;
        };

        LoanApplication application = LoanApplication.builder()
                .customerId(request.getCustomerId())
                .accountId(request.getAccountId())
                .loanType(request.getLoanType())
                .requestedAmount(request.getRequestedAmount())
                .termMonths(request.getTermMonths())
                .annualIncome(request.getAnnualIncome())
                .creditScore(request.getCreditScore())
                .status(status)
                .aiDecision(eligibility.getDecision())
                .aiReason(eligibility.getReason())
                .decidedAt(Instant.now())
                .build();

        LoanApplication saved = loanRepository.save(application);

        // Publish status change event to Kafka → notification-service
        kafkaTemplate.send("nexabank.loan.status.changed",
                saved.getId().toString(),
                new LoanStatusChangedEvent(
                        saved.getId(), saved.getCustomerId(),
                        LoanStatus.SUBMITTED.name(), status.name(),
                        eligibility.getDecision(), Instant.now()
                ));

        log.info("Loan application {} — decision: {} via {}",
                saved.getId(), eligibility.getDecision(), eligibility.getStrategyUsed());

        return toResponse(saved, eligibility.getStrategyUsed());
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(Long id) {
        LoanApplication app = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan application not found: " + id));
        return toResponse(app, null);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getByCustomer(Long customerId) {
        return loanRepository.findByCustomerIdOrderByAppliedAtDesc(customerId)
                .stream().map(a -> toResponse(a, null)).toList();
    }

    @Transactional
    public LoanApplicationResponse updateStatus(Long id, LoanStatus newStatus, String reviewedBy) {
        LoanApplication app = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan application not found: " + id));
        String previousStatus = app.getStatus().name();
        app.setStatus(newStatus);
        app.setReviewedBy(reviewedBy);
        app.setDecidedAt(Instant.now());
        LoanApplication saved = loanRepository.save(app);

        kafkaTemplate.send("nexabank.loan.status.changed", id.toString(),
                new LoanStatusChangedEvent(id, app.getCustomerId(),
                        previousStatus, newStatus.name(), app.getAiDecision(), Instant.now()));

        return toResponse(saved, null);
    }

    /**
     * Strategy selection — try AI first, fall back to rules.
     */
    private EligibilityResult evaluate(LoanApplicationRequest request) {
        try {
            return aiStrategy.evaluate(request);
        } catch (RestClientException e) {
            log.warn("AI layer unavailable — using rule-based strategy: {}", e.getMessage());
            return ruleStrategy.evaluate(request);
        }
    }

    private LoanApplicationResponse toResponse(LoanApplication a, String strategyUsed) {
        return LoanApplicationResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomerId())
                .accountId(a.getAccountId())
                .loanType(a.getLoanType())
                .requestedAmount(a.getRequestedAmount())
                .termMonths(a.getTermMonths())
                .annualIncome(a.getAnnualIncome())
                .creditScore(a.getCreditScore())
                .status(a.getStatus())
                .aiDecision(a.getAiDecision())
                .aiReason(a.getAiReason())
                .strategyUsed(strategyUsed)
                .appliedAt(a.getAppliedAt())
                .decidedAt(a.getDecidedAt())
                .build();
    }
}
