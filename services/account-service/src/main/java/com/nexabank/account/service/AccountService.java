package com.nexabank.account.service;

import com.nexabank.account.dto.AccountRequest;
import com.nexabank.account.dto.AccountResponse;
import com.nexabank.account.dto.BalanceResponse;
import com.nexabank.account.exception.AccountNotFoundException;
import com.nexabank.account.mapper.AccountMapper;
import com.nexabank.account.model.*;
import com.nexabank.account.repository.AccountRepository;
import com.nexabank.account.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountNumberFactory accountNumberFactory;
    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findByIdWithCustomer(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    @Transactional
    public BalanceResponse updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.setBalance(newBalance);
        accountRepository.save(account);
        log.info("Balance updated for account {}: {}", accountId, newBalance);
        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    @Transactional
    public AccountResponse openAccount(Long customerId, AccountRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new AccountNotFoundException("Customer not found: " + customerId));

        BigDecimal initialBalance = request.getInitialDeposit() != null
                ? request.getInitialDeposit()
                : BigDecimal.ZERO;

        BigDecimal interestRate = request.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("0.0250")   // 2.5% for savings
                : null;

        Account account = Account.builder()
                .accountNumber(accountNumberFactory.generate(request.getAccountType()))
                .accountType(request.getAccountType())
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .interestRate(interestRate)
                .customer(customer)
                .build();

        Account saved = accountRepository.save(account);
        log.info("New {} account {} opened for customer {}",
                request.getAccountType(), saved.getAccountNumber(), customerId);

        return accountMapper.toResponse(saved);
    }
}
