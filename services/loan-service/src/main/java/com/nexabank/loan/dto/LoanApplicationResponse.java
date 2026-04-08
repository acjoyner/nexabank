package com.nexabank.loan.dto;

import com.nexabank.loan.model.LoanStatus;
import com.nexabank.loan.model.LoanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class LoanApplicationResponse {
    private Long id;
    private Long customerId;
    private Long accountId;
    private LoanType loanType;
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private BigDecimal annualIncome;
    private Integer creditScore;
    private LoanStatus status;
    private String aiDecision;
    private String aiReason;
    private String strategyUsed;
    private Instant appliedAt;
    private Instant decidedAt;
}
