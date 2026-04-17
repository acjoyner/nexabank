package com.nexabank.account.service;

import com.nexabank.account.dto.AccountResponse;
import com.nexabank.account.dto.BalanceResponse;
import com.nexabank.account.exception.AccountNotFoundException;
import com.nexabank.account.model.*;
import com.nexabank.account.repository.AccountRepository;
import com.nexabank.account.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountNumberFactory accountNumberFactory;

    private AccountService accountService;

    private Customer testCustomer;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, customerRepository, accountNumberFactory, null);

        testCustomer = Customer.builder()
                .id(1L).email("alice@nexabank.com")
                .firstName("Alice").lastName("Smith")
                .status(CustomerStatus.ACTIVE)
                .build();

        testAccount = Account.builder()
                .id(100L).accountNumber("CHK-0000001001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1500.00"))
                .status(AccountStatus.ACTIVE)
                .customer(testCustomer)
                .build();
    }

    @Test
    void getAccountsByCustomer_returnsAccountList() {
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(testAccount));

        List<AccountResponse> result = accountService.getAccountsByCustomer(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("CHK-0000001001");
    }

    @Test
    void getBalance_returnsCurrentBalance() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

        BalanceResponse response = accountService.getBalance(100L);

        assertThat(response.getBalance()).isEqualByComparingTo("1500.00");
        assertThat(response.getAccountNumber()).isEqualTo("CHK-0000001001");
    }

    @Test
    void getAccount_unknownId_throwsAccountNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updateBalance_persistsNewBalance() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BalanceResponse response = accountService.updateBalance(100L, new BigDecimal("2000.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("2000.00");
        verify(accountRepository).save(testAccount);
    }

    @Test
    void getAccountsByCustomer_emptyList_whenNoAccounts() {
        when(accountRepository.findByCustomerId(99L)).thenReturn(List.of());

        List<AccountResponse> result = accountService.getAccountsByCustomer(99L);

        assertThat(result).isEmpty();
    }
}
