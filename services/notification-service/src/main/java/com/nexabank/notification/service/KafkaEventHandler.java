package com.nexabank.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kafka Consumer — handles events from Kafka topics.
 *
 * Each @KafkaListener method runs in a separate thread managed by
 * ConcurrentKafkaListenerContainerFactory (see KafkaConsumerConfig).
 *
 * Consumer group "notification-group" ensures that:
 * - Only ONE instance of this service processes each message partition
 * - If multiple instances are running, partitions are distributed between them
 *   (horizontal scaling — each instance handles a subset of partitions)
 *
 * Messages are deserialized from JSON to Map<String,Object> to avoid tight
 * coupling — the notification service doesn't import event record classes
 * from other services (no shared library needed for this demo).
 *
 * See docs/learning/03-kafka-event-streaming.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventHandler {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "nexabank.transaction.completed",
        groupId = "notification-group"
    )
    public void handleTransactionCompleted(ConsumerRecord<String, String> record) {
        try {
            Map<?, ?> event = objectMapper.readValue(record.value(), Map.class);
            Long customerId = extractCustomerId(event);
            String ref = (String) event.get("referenceNumber");
            String type = (String) event.get("type");
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

            String subject = String.format("Transaction Alert: %s of $%.2f", type, amount);
            String body = String.format(
                "Your %s transaction of $%.2f has been %s. Reference: %s",
                type, amount, event.get("status"), ref
            );

            notificationService.create(customerId, "TXN_COMPLETED", subject, body,
                    "nexabank.transaction.completed", ref);

            log.info("Kafka: notification created for transaction {}", ref);
        } catch (Exception e) {
            log.error("Kafka: failed to process transaction.completed event", e);
        }
    }

    @KafkaListener(
        topics = "nexabank.account.created",
        groupId = "notification-group"
    )
    public void handleAccountCreated(ConsumerRecord<String, String> record) {
        try {
            Map<?, ?> event = objectMapper.readValue(record.value(), Map.class);
            Long customerId = Long.valueOf(event.get("customerId").toString());
            String fullName = (String) event.get("fullName");
            String accountNumber = (String) event.get("accountNumber");

            String subject = "Welcome to NexaBank, " + fullName + "!";
            String body = String.format(
                "Welcome! Your checking account %s is now active. " +
                "Start banking securely with NexaBank.", accountNumber
            );

            notificationService.create(customerId, "ACCOUNT_CREATED", subject, body,
                    "nexabank.account.created", accountNumber);

            log.info("Kafka: welcome notification created for customer {}", customerId);
        } catch (Exception e) {
            log.error("Kafka: failed to process account.created event", e);
        }
    }

    @KafkaListener(
        topics = "nexabank.loan.status.changed",
        groupId = "notification-group"
    )
    public void handleLoanStatusChanged(ConsumerRecord<String, String> record) {
        try {
            Map<?, ?> event = objectMapper.readValue(record.value(), Map.class);
            Long customerId = Long.valueOf(event.get("customerId").toString());
            String status = (String) event.get("newStatus");
            Long loanId = Long.valueOf(event.get("loanId").toString());

            String subject = "Loan Application Update";
            String body = String.format(
                "Your loan application #%d status has been updated to: %s", loanId, status
            );

            notificationService.create(customerId, "LOAN_STATUS_CHANGED", subject, body,
                    "nexabank.loan.status.changed", loanId.toString());

            log.info("Kafka: loan status notification created for customer {}", customerId);
        } catch (Exception e) {
            log.error("Kafka: failed to process loan.status.changed event", e);
        }
    }

    private Long extractCustomerId(Map<?, ?> event) {
        Object id = event.get("customerId");
        return id != null ? Long.valueOf(id.toString()) : 0L;
    }
}
