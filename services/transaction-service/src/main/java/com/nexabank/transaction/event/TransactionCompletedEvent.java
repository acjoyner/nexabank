package com.nexabank.transaction.event;

import com.nexabank.transaction.model.TransactionType;
import com.nexabank.transaction.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kafka event published after a transaction completes.
 *
 * Published to topic: nexabank.transaction.completed (6 partitions)
 * Consumed by: notification-service (sends email/SMS alert)
 *
 * Java record ensures immutability — events should never be mutated.
 * The referenceNumber links back to the TRANSACTIONS table for audit.
 *
 * See docs/learning/03-kafka-event-streaming.md
 */
public record TransactionCompletedEvent(
        Long transactionId,
        String referenceNumber,
        Long sourceAccountId,
        Long destAccountId,         // null for deposits/withdrawals
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String createdBy,           // customer email
        Instant occurredAt
) {}
