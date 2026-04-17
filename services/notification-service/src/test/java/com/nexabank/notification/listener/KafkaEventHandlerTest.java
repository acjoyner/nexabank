package com.nexabank.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexabank.notification.service.NotificationService;
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

        handler.handleTransactionCompleted(payload);

        verify(notificationService).create(
                anyLong(), eq("TRANSACTION"), anyString(), anyString(),
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

        handler.handleAccountCreated(payload);

        verify(notificationService).create(
                anyLong(), eq("ACCOUNT"), anyString(), anyString(),
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

        handler.handleLoanStatusChanged(payload);

        verify(notificationService).create(
                anyLong(), eq("LOAN"), anyString(), anyString(),
                eq("nexabank.loan.status.changed"), any());
    }
}
