package com.nexabank.transaction.service;

import com.nexabank.transaction.client.AccountServiceClient;
import com.nexabank.transaction.dto.DepositRequest;
import com.nexabank.transaction.dto.TransactionResponse;
import com.nexabank.transaction.dto.TransferRequest;
import com.nexabank.transaction.dto.WithdrawalRequest;
import com.nexabank.transaction.exception.InsufficientFundsException;
import com.nexabank.transaction.model.TransactionStatus;
import com.nexabank.transaction.model.TransactionType;
import com.nexabank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerService ledgerService;
    @Mock private AccountServiceClient accountServiceClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private MqDispatchService mqDispatchService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, ledgerService, accountServiceClient,
                kafkaTemplate, mqDispatchService);
    }

    @Test
    void deposit_createsCompletedTransaction() {
        DepositRequest req = new DepositRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Paycheck");

        mockBalance(1L, "1000.00");
        mockUpdateBalance(1L, "1500.00");
        mockSaveTransaction();

        TransactionResponse response = transactionService.deposit(req, "alice@nexabank.com");

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void withdrawal_sufficientFunds_succeeds() {
        WithdrawalRequest req = new WithdrawalRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("200.00"));
        req.setDescription("ATM");

        mockBalance(1L, "1000.00");
        mockUpdateBalance(1L, "800.00");
        mockSaveTransaction();

        TransactionResponse response = transactionService.withdrawal(req, "alice@nexabank.com");

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void withdrawal_insufficientFunds_throwsException() {
        WithdrawalRequest req = new WithdrawalRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("9999.00"));

        mockBalance(1L, "100.00");

        assertThatThrownBy(() -> transactionService.withdrawal(req, "alice@nexabank.com"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(1L);
        req.setDestAccountId(2L);
        req.setAmount(new BigDecimal("300.00"));
        req.setDescription("Rent");

        mockBalance(1L, "1000.00");
        mockBalance(2L, "500.00");
        mockUpdateBalance(1L, "700.00");
        mockUpdateBalance(2L, "800.00");
        mockSaveTransaction();

        TransactionResponse response = transactionService.transfer(req, "alice@nexabank.com");

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(kafkaTemplate).send(eq("nexabank.transaction.completed"), any(), any());
        verify(mqDispatchService).dispatch(any());
    }

    @Test
    void deposit_publishesKafkaEvent() {
        DepositRequest req = new DepositRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("100.00"));

        mockBalance(1L, "500.00");
        mockUpdateBalance(1L, "600.00");
        mockSaveTransaction();

        transactionService.deposit(req, "user@nexabank.com");

        verify(kafkaTemplate).send(eq("nexabank.transaction.completed"), any(), any());
    }

    private void mockBalance(Long accountId, String balance) {
        var balanceResponse = mock(com.nexabank.transaction.client.AccountServiceClient.BalanceResponse.class);
        when(balanceResponse.getBalance()).thenReturn(new BigDecimal(balance));
        when(balanceResponse.getStatus()).thenReturn("ACTIVE");
        when(accountServiceClient.getBalance(accountId)).thenReturn(balanceResponse);
    }

    private void mockUpdateBalance(Long accountId, String newBalance) {
        var balanceResponse = mock(com.nexabank.transaction.client.AccountServiceClient.BalanceResponse.class);
        when(balanceResponse.getBalance()).thenReturn(new BigDecimal(newBalance));
        when(accountServiceClient.updateBalance(eq(accountId), any(Map.class))).thenReturn(balanceResponse);
    }

    private void mockSaveTransaction() {
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            var t = inv.getArgument(0, com.nexabank.transaction.model.Transaction.class);
            t.setId(1L);
            return t;
        });
    }
}
