package com.nexabank.account.repository;

import com.nexabank.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Eagerly fetch account with customer — solves the N+1 Select Problem.
     * Without JOIN FETCH, accessing account.getCustomer() triggers a separate query per account.
     */
    @Query("SELECT a FROM Account a JOIN FETCH a.customer WHERE a.id = :id")
    Optional<Account> findByIdWithCustomer(@Param("id") Long id);

    List<Account> findByCustomerId(Long customerId);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
