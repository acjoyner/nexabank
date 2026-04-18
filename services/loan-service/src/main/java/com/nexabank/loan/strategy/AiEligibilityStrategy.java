package com.nexabank.loan.strategy;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * AI Eligibility Strategy — delegates to Python FastAPI AI layer.
 *
 * Uses Spring Boot 3's RestClient (modern synchronous HTTP client
 * that replaces RestTemplate — introduced in Spring Boot 3.2).
 *
 * Calls POST http://ai-layer:8000/api/loan/eligibility with the
 * loan application data and maps the response to EligibilityResult.
 *
 * If the AI layer is unavailable, throws RestClientException which
 * LoanApplicationService catches to fall back to RuleBasedEligibilityStrategy.
 *
 * See docs/learning/11-design-patterns-used.md
 */
@Component("aiStrategy")
@RequiredArgsConstructor
@Slf4j
public class AiEligibilityStrategy implements LoanEligibilityStrategy {

    private final RestClient restClient;
    private final LoanEligibilityStrategy fallback;

    @Value("${ai-layer.url:http://localhost:8000}")
    private String aiLayerUrl;

    public AiEligibilityStrategy(RestClient restClient, LoanEligibilityStrategy fallback) {
        this.restClient = restClient;
        this.fallback = fallback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EligibilityResult evaluate(LoanApplicationRequest request) {
        log.info("Evaluating loan eligibility via AI layer");

        try {
            Map<String, Object> aiRequest = Map.of(
                    "loan_type", request.getLoanType().name(),
                    "requested_amount", request.getRequestedAmount(),
                    "term_months", request.getTermMonths(),
                    "annual_income", request.getAnnualIncome(),
                    "credit_score", request.getCreditScore() != null ? request.getCreditScore() : 0
            );

            Map<String, Object> response = restClient.post()
                    .uri(aiLayerUrl + "/api/loan/eligibility")
                    .body(aiRequest)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RestClientException("Empty response from AI layer");
            }

            String decision = (String) response.getOrDefault("decision", "REVIEW");
            String reason   = (String) response.getOrDefault("reason", "AI assessment complete");
            log.info("AI layer decision: {} — {}", decision, reason);

            return EligibilityResult.builder()
                    .decision(decision)
                    .reason(reason)
                    .strategyUsed("AI")
                    .build();
        } catch (Exception e) {
            log.warn("AI layer unavailable, falling back to rule-based strategy", e);
            return fallback.evaluate(request);
        }
    }
}
