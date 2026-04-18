package com.nexabank.loan.strategy;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.model.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiEligibilityStrategyTest {

    @Mock private RestClient restClient;
    @Mock private RuleBasedEligibilityStrategy fallback;

    private AiEligibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AiEligibilityStrategy(restClient, fallback);
    }

    @Test
    void aiUnavailable_fallsBackToRuleBasedStrategy() {
        LoanApplicationRequest req = buildRequest(LoanType.AUTO, 20000, 60, 60000, 700);
        EligibilityResult ruleResult = EligibilityResult.builder()
                .decision("ELIGIBLE")
                .reason("Rule-based approval")
                .strategyUsed("RuleBased")
                .build();

        when(restClient.post()).thenThrow(new RestClientException("Connection refused"));
        when(fallback.evaluate(req)).thenReturn(ruleResult);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isEqualTo("ELIGIBLE");
        assertThat(result.getStrategyUsed()).containsIgnoringCase("rule");
        verify(fallback).evaluate(req);
    }

    @Test
    void fallbackResult_containsFallbackIndicator() {
        LoanApplicationRequest req = buildRequest(LoanType.PERSONAL, 10000, 24, 50000, 650);
        EligibilityResult ruleResult = new EligibilityResult("REVIEW", "Borderline", "RuleBased");

        when(restClient.post()).thenThrow(new RestClientException("timeout"));
        when(fallback.evaluate(req)).thenReturn(ruleResult);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getStrategyUsed()).doesNotContain("AI");
    }

    private LoanApplicationRequest buildRequest(LoanType type, double amount,
                                                  int months, double income, int creditScore) {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerId(1L);
        req.setAccountId(100L);
        req.setLoanType(type);
        req.setRequestedAmount(BigDecimal.valueOf(amount));
        req.setTermMonths(months);
        req.setAnnualIncome(BigDecimal.valueOf(income));
        req.setCreditScore(creditScore);
        return req;
    }
}
