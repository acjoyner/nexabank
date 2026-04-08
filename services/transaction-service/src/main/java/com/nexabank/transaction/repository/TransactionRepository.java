package com.nexabank.transaction.repository;

import com.nexabank.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceAccountIdOrderByCreatedAtDesc(Long sourceAccountId);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);
}
