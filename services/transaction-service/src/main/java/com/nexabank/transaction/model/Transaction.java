package com.nexabank.transaction.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction entity — every money movement record.
 *
 * REFERENCE_NUMBER is a UUID for idempotency and correlation across services.
 * CREATED_BY stores the customer email from the JWT (forwarded by gateway as X-User-Email).
 */
@Entity
@Table(name = "TRANSACTIONS", indexes = {
    @Index(name = "IDX_TXN_SOURCE_ACCOUNT", columnList = "SOURCE_ACCOUNT_ID"),
    @Index(name = "IDX_TXN_CREATED_AT", columnList = "CREATED_AT")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "txn_seq")
    @SequenceGenerator(name = "txn_seq", sequenceName = "SEQ_TRANSACTION_ID", allocationSize = 1)
    private Long id;

    @Column(name = "REFERENCE_NUMBER", nullable = false, unique = true, length = 36)
    private String referenceNumber;  // UUID

    @Column(name = "SOURCE_ACCOUNT_ID", nullable = false)
    private Long sourceAccountId;

    @Column(name = "DEST_ACCOUNT_ID")
    private Long destAccountId;  // null for deposits/withdrawals

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 500)
    private String description;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "COMPLETED_AT")
    private Instant completedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 255)
    private String createdBy;  // customer email from JWT

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
