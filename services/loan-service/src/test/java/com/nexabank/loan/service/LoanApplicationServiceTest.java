package com.nexabank.loan.service;

import com.nexabank.loan.dto.EligibilityResult;
import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.dto.LoanApplicationResponse;
import com.nexabank.loan.model.LoanApplication;
import com.nexabank.loan.model.LoanStatus;
import com.nexabank.loan.model.LoanType;
import com.nexabank.loan.repository.LoanApplicationRepository;
import com.nexabank.loan.strategy.AiEligibilityStrategy;
import com.nexabank.loan.strategy.RuleBasedEligibilityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private AiEligibilityStrategy aiStrategy;
    @Mock private RuleBasedEligibilityStrategy ruleBasedStrategy;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private LoanApplicationService loanApplicationService;

    @BeforeEach
    void setUp() {
        loanApplicationService = new LoanApplicationService(
                loanApplicationRepository, aiStrategy, ruleBasedStrategy, kafkaTemplate);
    }

    @Test
    void apply_persistsLoanWithAiDecision() {
        LoanApplicationRequest req = buildRequest(LoanType.AUTO, 20000, 60, 70000, 720);
        EligibilityResult aiResult = new EligibilityResult("ELIGIBLE", "AI approved", "AI");

        when(aiStrategy.evaluate(req)).thenReturn(aiResult);
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
            LoanApplication la = inv.getArgument(0);
            la.setId(1L);
            return la;
        });

        LoanApplicationResponse response = loanApplicationService.apply(req);

        assertThat(response.getAiDecision()).isEqualTo("ELIGIBLE");
        assertThat(response.getStatus()).isEqualTo(LoanStatus.AI_REVIEW);
        verify(kafkaTemplate).send(eq("nexabank.loan.status.changed"), any(), any());
    }

    @Test
    void apply_aiDecisionPersisted_inAiDecisionField() {
        LoanApplicationRequest req = buildRequest(LoanType.PERSONAL, 5000, 12, 40000, 580);
        EligibilityResult aiResult = new EligibilityResult("INELIGIBLE", "Low credit score", "AI");

        when(aiStrategy.evaluate(req)).thenReturn(aiResult);
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
            LoanApplication la = inv.getArgument(0);
            la.setId(2L);
            return la;
        });

        LoanApplicationResponse response = loanApplicationService.apply(req);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationRepository).save(captor.capture());
        assertThat(captor.getValue().getAiDecision()).isEqualTo("INELIGIBLE");
    }

    @Test
    void getById_returnsCorrectLoan() {
        LoanApplication la = buildLoanApplication(10L, LoanStatus.APPROVED);
        when(loanApplicationRepository.findById(10L)).thenReturn(Optional.of(la));

        LoanApplicationResponse result = loanApplicationService.getById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
    }

    @Test
    void getById_notFound_throwsException() {
        when(loanApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.getById(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getByCustomer_returnsAllLoans() {
        when(loanApplicationRepository.findByCustomerIdOrderByAppliedAtDesc(42L))
                .thenReturn(List.of(buildLoanApplication(1L, LoanStatus.SUBMITTED),
                        buildLoanApplication(2L, LoanStatus.APPROVED)));

        List<LoanApplicationResponse> result = loanApplicationService.getByCustomer(42L);

        assertThat(result).hasSize(2);
    }

    @Test
    void updateStatus_changesStatusAndPublishesEvent() {
        LoanApplication la = buildLoanApplication(5L, LoanStatus.AI_REVIEW);
        when(loanApplicationRepository.findById(5L)).thenReturn(Optional.of(la));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationResponse result = loanApplicationService.updateStatus(5L, LoanStatus.APPROVED, "admin@nexabank.com");

        assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
        verify(kafkaTemplate).send(eq("nexabank.loan.status.changed"), any(), any());
    }

    private LoanApplicationRequest buildRequest(LoanType type, double amount, int months,
                                                  double income, int creditScore) {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setCustomerId(42L);
        req.setAccountId(100L);
        req.setLoanType(type);
        req.setRequestedAmount(BigDecimal.valueOf(amount));
        req.setTermMonths(months);
        req.setAnnualIncome(BigDecimal.valueOf(income));
        req.setCreditScore(creditScore);
        return req;
    }

    private LoanApplication buildLoanApplication(Long id, LoanStatus status) {
        LoanApplication la = new LoanApplication();
        la.setId(id);
        la.setCustomerId(42L);
        la.setStatus(status);
        la.setLoanType(LoanType.AUTO);
        la.setRequestedAmount(new BigDecimal("20000"));
        la.setTermMonths(60);
        la.setAnnualIncome(new BigDecimal("70000"));
        return la;
    }
}
