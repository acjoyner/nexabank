package com.nexabank.transaction.repository;

import com.nexabank.transaction.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountIdOrderByPostedAtDesc(Long accountId);

    List<LedgerEntry> findByTransactionId(Long transactionId);
}
