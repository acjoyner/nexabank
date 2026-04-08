package com.nexabank.account.event;

import java.time.Instant;

/**
 * Kafka event published when a new account is created.
 *
 * Uses Java record (immutable value object — Java 17 feature).
 * Published to topic: nexabank.account.created
 * Consumed by: notification-service (sends welcome email)
 *
 * See docs/learning/03-kafka-event-streaming.md
 */
public record AccountCreatedEvent(
        Long customerId,
        String email,
        String fullName,
        Long accountId,
        String accountNumber,
        String accountType,
        Instant occurredAt
) {}
