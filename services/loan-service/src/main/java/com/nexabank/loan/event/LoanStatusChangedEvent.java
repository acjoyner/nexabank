package com.nexabank.loan.event;

import java.time.Instant;

/**
 * Kafka event published when a loan application status changes.
 * Published to: nexabank.loan.status.changed
 * Consumed by: notification-service
 */
public record LoanStatusChangedEvent(
        Long loanId,
        Long customerId,
        String previousStatus,
        String newStatus,
        String aiDecision,
        Instant occurredAt
) {}
