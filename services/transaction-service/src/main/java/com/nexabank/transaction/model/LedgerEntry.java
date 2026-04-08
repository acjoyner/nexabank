package com.nexabank.transaction.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger Entry — double-entry bookkeeping.
 *
 * Every transaction produces 2 ledger entries:
 * - DEBIT from source account
 * - CREDIT to destination account
 *
 * This mirrors how real banking ledgers work and allows full audit trails.
 * BALANCE_AFTER captures the account balance at the moment of the entry.
 */
@Entity
@Table(name = "LEDGER_ENTRIES", indexes = {
    @Index(name = "IDX_LEDGER_ACCOUNT",     columnList = "ACCOUNT_ID"),
    @Index(name = "IDX_LEDGER_TRANSACTION",  columnList = "TRANSACTION_ID")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_seq")
    @SequenceGenerator(name = "ledger_seq", sequenceName = "SEQ_LEDGER_ID", allocationSize = 1)
    private Long id;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "ENTRY_TYPE", nullable = false, length = 6)
    private String entryType;  // DEBIT or CREDIT

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "BALANCE_AFTER", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "POSTED_AT", nullable = false, updatable = false)
    private Instant postedAt;

    @PrePersist
    protected void onCreate() {
        postedAt = Instant.now();
    }
}
