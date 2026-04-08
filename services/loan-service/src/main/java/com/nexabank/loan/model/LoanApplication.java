package com.nexabank.loan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "LOAN_APPLICATIONS", indexes = {
    @Index(name = "IDX_LOAN_CUSTOMER", columnList = "CUSTOMER_ID"),
    @Index(name = "IDX_LOAN_STATUS",   columnList = "STATUS")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    @SequenceGenerator(name = "loan_seq", sequenceName = "SEQ_LOAN_ID", allocationSize = 1)
    private Long id;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_TYPE", nullable = false, length = 30)
    private LoanType loanType;

    @Column(name = "REQUESTED_AMOUNT", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "TERM_MONTHS", nullable = false)
    private Integer termMonths;

    @Column(name = "ANNUAL_INCOME", nullable = false, precision = 19, scale = 4)
    private BigDecimal annualIncome;

    @Column(name = "CREDIT_SCORE")
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoanStatus status = LoanStatus.SUBMITTED;

    @Column(name = "AI_DECISION", length = 20)
    private String aiDecision;   // ELIGIBLE, INELIGIBLE, REVIEW

    @Column(name = "AI_REASON", columnDefinition = "TEXT")
    private String aiReason;

    @Column(name = "REVIEWED_BY", length = 255)
    private String reviewedBy;

    @Column(name = "APPLIED_AT", nullable = false, updatable = false)
    private Instant appliedAt;

    @Column(name = "DECIDED_AT")
    private Instant decidedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = Instant.now();
    }
}
