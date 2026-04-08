package com.nexabank.transaction.dto;

import com.nexabank.transaction.model.TransactionStatus;
import com.nexabank.transaction.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String referenceNumber;
    private Long sourceAccountId;
    private Long destAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;
    private Instant completedAt;
}
