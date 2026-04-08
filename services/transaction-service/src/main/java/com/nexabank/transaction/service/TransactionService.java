package com.nexabank.transaction.service;

import com.nexabank.transaction.client.AccountServiceClient;
import com.nexabank.transaction.dto.*;
import com.nexabank.transaction.event.TransactionCompletedEvent;
import com.nexabank.transaction.exception.AccountFrozenException;
import com.nexabank.transaction.exception.InsufficientFundsException;
import com.nexabank.transaction.model.*;
import com.nexabank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transaction Service — core money movement business logic.
 *
 * The transfer() method is the most complex operation:
 * 1. Validate both accounts via Feign (with circuit breaker fallback)
 * 2. Check sufficient balance
 * 3. Persist Transaction + 2 LedgerEntries (@Transactional — all-or-nothing)
 * 4. Publish to Kafka (audit/notification stream)
 * 5. Dispatch to ActiveMQ (guaranteed MQ delivery)
 *
 * SAGA PATTERN (simplified):
 * If Kafka/MQ fails AFTER the DB commit, the transaction is already recorded
 * (eventual consistency). A compensating transaction would be needed to reverse.
 * The referenceNumber UUID enables idempotency for retry scenarios.
 *
 * See docs/learning/03-kafka-event-streaming.md
 * See docs/learning/04-activemq-mq-messaging.md
 * See docs/learning/11-design-patterns-used.md (Saga section)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MqDispatchService mqDispatchService;

    @Transactional
    public TransactionResponse deposit(DepositRequest request, String customerEmail) {
        AccountServiceClient.BalanceResponse account =
                accountServiceClient.getBalance(request.getAccountId());
        validateAccountActive(request.getAccountId(), account.getStatus());

        Transaction txn = transactionRepository.save(Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .sourceAccountId(request.getAccountId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(Instant.now())
                .createdBy(customerEmail)
                .build());

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        ledgerService.recordCredit(txn.getId(), request.getAccountId(),
                request.getAmount(), newBalance);
        accountServiceClient.updateBalance(request.getAccountId(), Map.of("balance", newBalance));

        publishEvents(txn, customerEmail);
        log.info("Deposit {} completed: ${} to account {}, new balance: {}",
                txn.getReferenceNumber(), request.getAmount(), request.getAccountId(), newBalance);
        return toResponse(txn);
    }

    @Transactional
    public TransactionResponse withdrawal(WithdrawalRequest request, String customerEmail) {
        AccountServiceClient.BalanceResponse account =
                accountServiceClient.getBalance(request.getAccountId());
        validateAccountActive(request.getAccountId(), account.getStatus());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    request.getAccountId(), account.getBalance(), request.getAmount());
        }

        Transaction txn = transactionRepository.save(Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .sourceAccountId(request.getAccountId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(Instant.now())
                .createdBy(customerEmail)
                .build());

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        ledgerService.recordDebit(txn.getId(), request.getAccountId(),
                request.getAmount(), newBalance);
        accountServiceClient.updateBalance(request.getAccountId(), Map.of("balance", newBalance));

        publishEvents(txn, customerEmail);
        log.info("Withdrawal {} completed: ${} from account {}, new balance: {}",
                txn.getReferenceNumber(), request.getAmount(), request.getAccountId(), newBalance);
        return toResponse(txn);
    }

    /**
     * Transfer — most complex operation. Demonstrates:
     * - Cross-service validation (Feign to account-service)
     * - @Transactional ensuring atomicity of 2 ledger entries
     * - Both Kafka (streaming) and ActiveMQ (guaranteed delivery) publishing
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String customerEmail) {
        AccountServiceClient.BalanceResponse source =
                accountServiceClient.getBalance(request.getSourceAccountId());
        AccountServiceClient.BalanceResponse dest =
                accountServiceClient.getBalance(request.getDestAccountId());

        validateAccountActive(request.getSourceAccountId(), source.getStatus());
        validateAccountActive(request.getDestAccountId(), dest.getStatus());

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    request.getSourceAccountId(), source.getBalance(), request.getAmount());
        }

        Transaction txn = transactionRepository.save(Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .sourceAccountId(request.getSourceAccountId())
                .destAccountId(request.getDestAccountId())
                .transactionType(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(Instant.now())
                .createdBy(customerEmail)
                .build());

        // Double-entry: debit source, credit destination
        BigDecimal newSourceBalance = source.getBalance().subtract(request.getAmount());
        BigDecimal newDestBalance = dest.getBalance().add(request.getAmount());
        ledgerService.recordDebit(txn.getId(), request.getSourceAccountId(),
                request.getAmount(), newSourceBalance);
        ledgerService.recordCredit(txn.getId(), request.getDestAccountId(),
                request.getAmount(), newDestBalance);
        accountServiceClient.updateBalance(request.getSourceAccountId(), Map.of("balance", newSourceBalance));
        accountServiceClient.updateBalance(request.getDestAccountId(), Map.of("balance", newDestBalance));

        publishEvents(txn, customerEmail);
        log.info("Transfer {} completed: ${} from {} to {}",
                txn.getReferenceNumber(), request.getAmount(),
                request.getSourceAccountId(), request.getDestAccountId());
        return toResponse(txn);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrderByCreatedAtDesc(accountId)
                .stream().map(this::toResponse).toList();
    }

    private void publishEvents(Transaction txn, String customerEmail) {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                txn.getId(), txn.getReferenceNumber(),
                txn.getSourceAccountId(), txn.getDestAccountId(),
                txn.getAmount(), txn.getCurrency(),
                txn.getTransactionType(), txn.getStatus(),
                customerEmail, Instant.now()
        );
        // Kafka: high-throughput streaming (6-partition topic)
        kafkaTemplate.send("nexabank.transaction.completed",
                txn.getReferenceNumber(), event);
        // ActiveMQ: guaranteed point-to-point delivery
        mqDispatchService.dispatch(event);
    }

    private void validateAccountActive(Long accountId, String status) {
        if ("FROZEN".equals(status) || "CLOSED".equals(status)) {
            throw new AccountFrozenException(accountId);
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .sourceAccountId(t.getSourceAccountId())
                .destAccountId(t.getDestAccountId())
                .transactionType(t.getTransactionType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
