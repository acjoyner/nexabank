package com.nexabank.transaction.service;

import com.nexabank.transaction.model.LedgerEntry;
import com.nexabank.transaction.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Ledger Service — double-entry bookkeeping.
 *
 * Every transaction creates 2 entries:
 * - DEBIT:  reduces source account (money going out)
 * - CREDIT: increases destination account (money coming in)
 *
 * This is standard accounting practice required in banking systems.
 * The BALANCE_AFTER field creates a point-in-time snapshot for audits.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public void recordDebit(Long transactionId, Long accountId,
                            BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .entryType("DEBIT")
                .amount(amount)
                .balanceAfter(balanceAfter)
                .build();
        ledgerEntryRepository.save(entry);
    }

    public void recordCredit(Long transactionId, Long accountId,
                             BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .entryType("CREDIT")
                .amount(amount)
                .balanceAfter(balanceAfter)
                .build();
        ledgerEntryRepository.save(entry);
    }
}
