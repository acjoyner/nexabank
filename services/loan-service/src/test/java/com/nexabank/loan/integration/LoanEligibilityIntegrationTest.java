package com.nexabank.loan.integration;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.model.LoanType;
import com.nexabank.loan.strategy.RuleBasedEligibilityStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoanEligibilityIntegrationTest {

    @Autowired private RuleBasedEligibilityStrategy ruleBasedStrategy;
    @MockBean  private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void ruleBasedStrategy_wiredCorrectly_evaluatesRequest() {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerId(1L);
        req.setAccountId(100L);
        req.setLoanType(LoanType.AUTO);
        req.setRequestedAmount(new BigDecimal("15000"));
        req.setTermMonths(36);
        req.setAnnualIncome(new BigDecimal("75000"));
        req.setCreditScore(750);

        EligibilityResult result = ruleBasedStrategy.evaluate(req);

        assertThat(result.getDecision()).isIn("ELIGIBLE", "REVIEW", "INELIGIBLE");
        assertThat(result.getReason()).isNotBlank();
        assertThat(result.getStrategyUsed()).isNotBlank();
    }
}
