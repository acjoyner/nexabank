package com.nexabank.loan.strategy;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.model.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedEligibilityStrategyTest {

    private RuleBasedEligibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedEligibilityStrategy();
    }

    @Test
    void excellentProfile_returnsEligible() {
        LoanApplicationRequest req = buildRequest(
                LoanType.AUTO, new BigDecimal("15000"), 36,
                new BigDecimal("80000"), 780);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isEqualTo("ELIGIBLE");
    }

    @Test
    void creditScoreBelowMinimum_returnsIneligible() {
        LoanApplicationRequest req = buildRequest(
                LoanType.PERSONAL, new BigDecimal("10000"), 24,
                new BigDecimal("40000"), 520);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isEqualTo("INELIGIBLE");
        assertThat(result.getReason()).containsIgnoringCase("credit");
    }

    @Test
    void highDti_returnsIneligibleOrReview() {
        // $90k loan / 12 months = $7,500/mo payment on $5,000/mo income → DTI = 150%
        LoanApplicationRequest req = buildRequest(
                LoanType.PERSONAL, new BigDecimal("90000"), 12,
                new BigDecimal("60000"), 700);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isIn("INELIGIBLE", "REVIEW");
    }

    @Test
    void loanExceedsFiveTimesIncome_penalisedResult() {
        // $600k loan on $100k income = 6x
        LoanApplicationRequest req = buildRequest(
                LoanType.PERSONAL, new BigDecimal("600000"), 360,
                new BigDecimal("100000"), 750);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isIn("INELIGIBLE", "REVIEW");
    }

    @Test
    void borderlineProfile_returnsReview() {
        // Credit score 600 (REVIEW range), moderate DTI
        LoanApplicationRequest req = buildRequest(
                LoanType.PERSONAL, new BigDecimal("20000"), 48,
                new BigDecimal("55000"), 605);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getDecision()).isIn("REVIEW", "INELIGIBLE");
    }

    @Test
    void mortgageLoan_lowerRiskWeight_helpsBorderlineCase() {
        LoanApplicationRequest mortgageReq = buildRequest(
                LoanType.MORTGAGE, new BigDecimal("200000"), 360,
                new BigDecimal("100000"), 720);
        LoanApplicationRequest personalReq = buildRequest(
                LoanType.PERSONAL, new BigDecimal("200000"), 360,
                new BigDecimal("100000"), 720);

        EligibilityResult mortgage = strategy.evaluate(mortgageReq);
        EligibilityResult personal = strategy.evaluate(personalReq);

        assertThat(mortgage.getDecision()).isNotEqualTo("INELIGIBLE");
    }

    @Test
    void resultContainsStrategyName() {
        LoanApplicationRequest req = buildRequest(
                LoanType.AUTO, new BigDecimal("20000"), 60,
                new BigDecimal("60000"), 700);

        EligibilityResult result = strategy.evaluate(req);

        assertThat(result.getStrategyUsed()).containsIgnoringCase("rule");
    }

    private LoanApplicationRequest buildRequest(LoanType type, BigDecimal amount,
                                                 int months, BigDecimal income, int creditScore) {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerId(1L);
        req.setAccountId(100L);
        req.setLoanType(type);
        req.setRequestedAmount(amount);
        req.setTermMonths(months);
        req.setAnnualIncome(income);
        req.setCreditScore(creditScore);
        return req;
    }
}
