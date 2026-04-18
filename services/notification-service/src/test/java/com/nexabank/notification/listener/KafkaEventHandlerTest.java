package com.nexabank.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexabank.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaEventHandlerTest {

    @Mock private NotificationService notificationService;

    private KafkaEventHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new KafkaEventHandler(notificationService, objectMapper);
    }

    @Test
    void handleTransactionCompleted_createsNotification() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "transactionId", 1,
                "referenceNumber", "ref-abc",
                "sourceAccountId", 100,
                "amount", 500.00,
                "currency", "USD",
                "type", "TRANSFER",
                "status", "COMPLETED",
                "createdBy", "alice@nexabank.com",
                "occurredAt", Instant.now().toString()
        ));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("nexabank.transaction.completed", 0, 0, "key", payload);
        handler.handleTransactionCompleted(record);

        verify(notificationService).create(
                anyLong(), eq("TXN_COMPLETED"), anyString(), anyString(),
                eq("nexabank.transaction.completed"), any());
    }

    @Test
    void handleAccountCreated_createsWelcomeNotification() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "customerId", 42,
                "email", "alice@nexabank.com",
                "fullName", "Alice Smith",
                "accountId", 1001,
                "accountNumber", "CHK-0000001001",
                "accountType", "CHECKING",
                "occurredAt", Instant.now().toString()
        ));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("nexabank.account.created", 0, 0, "key", payload);
        handler.handleAccountCreated(record);

        verify(notificationService).create(
                anyLong(), eq("ACCOUNT_CREATED"), anyString(), anyString(),
                eq("nexabank.account.created"), any());
    }

    @Test
    void handleLoanStatusChanged_createsLoanNotification() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "loanId", 5,
                "customerId", 42,
                "previousStatus", "SUBMITTED",
                "newStatus", "APPROVED",
                "aiDecision", "ELIGIBLE",
                "occurredAt", Instant.now().toString()
        ));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("nexabank.loan.status.changed", 0, 0, "key", payload);
        handler.handleLoanStatusChanged(record);

        verify(notificationService).create(
                anyLong(), eq("LOAN_STATUS_CHANGED"), anyString(), anyString(),
                eq("nexabank.loan.status.changed"), any());
    }
}
