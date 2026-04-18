package com.nexabank.transaction.service;

import com.nexabank.transaction.model.LedgerEntry;
import com.nexabank.transaction.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock private LedgerEntryRepository ledgerEntryRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerEntryRepository);
    }

    @Test
    void recordDebit_savesEntryWithDebitType() {
        ledgerService.recordDebit(1L, 100L, new BigDecimal("500.00"), new BigDecimal("1000.00"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertThat(entry.getEntryType()).isEqualTo("DEBIT");
        assertThat(entry.getTransactionId()).isEqualTo(1L);
        assertThat(entry.getAccountId()).isEqualTo(100L);
        assertThat(entry.getAmount()).isEqualByComparingTo("500.00");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("1000.00");
    }

    @Test
    void recordCredit_savesEntryWithCreditType() {
        ledgerService.recordCredit(2L, 200L, new BigDecimal("250.00"), new BigDecimal("1750.00"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertThat(entry.getEntryType()).isEqualTo("CREDIT");
        assertThat(entry.getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    void doubleEntry_debitAndCredit_bothPersisted() {
        BigDecimal amount = new BigDecimal("100.00");
        ledgerService.recordDebit(3L, 10L, amount, new BigDecimal("900.00"));
        ledgerService.recordCredit(3L, 20L, amount, new BigDecimal("1100.00"));

        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
    }
}
