package com.nexabank.account.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Account entity — maps to ACCOUNTS table.
 *
 * Key design decisions:
 * - BigDecimal for balance (never use float/double for money — precision loss)
 * - NUMERIC(19,4) in the DB (supports up to $999 trillion with 4 decimal places)
 * - Oracle-compatible sequence (SEQ_ACCOUNT_ID starts at 1000)
 * - AccountType enum stored as String for readability in the DB
 *
 * See docs/learning/06-oracle-sql-schema.md for DDL explanation.
 */
@Entity
@Table(name = "ACCOUNTS", indexes = {
    @Index(name = "IDX_ACCOUNTS_CUSTOMER", columnList = "CUSTOMER_ID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq", sequenceName = "SEQ_ACCOUNT_ID",
                       allocationSize = 1)
    private Long id;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, unique = true, length = 20)
    private String accountNumber;  // e.g. CHK-0000001001, SAV-0000001002

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_TYPE", nullable = false, length = 20)
    private AccountType accountType;

    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "INTEREST_RATE", precision = 5, scale = 4)
    private BigDecimal interestRate;  // For SAVINGS accounts

    @Column(name = "OPENED_AT", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "CLOSED_AT")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @PrePersist
    protected void onCreate() {
        openedAt = Instant.now();
    }
}
