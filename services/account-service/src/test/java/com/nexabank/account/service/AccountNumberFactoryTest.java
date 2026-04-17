package com.nexabank.account.service;

import com.nexabank.account.model.AccountType;
import com.nexabank.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountNumberFactoryTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountNumberFactory factory;

    private long count = 0;

    @BeforeEach
    void setUp() {
        when(accountRepository.count()).thenAnswer(inv -> count++);
        factory = new AccountNumberFactory(accountRepository);
    }

    @Test
    void checkingAccount_hasChkPrefix() {
        String number = factory.generate(AccountType.CHECKING);
        assertThat(number).startsWith("CHK-");
    }

    @Test
    void savingsAccount_hasSavPrefix() {
        String number = factory.generate(AccountType.SAVINGS);
        assertThat(number).startsWith("SAV-");
    }

    @Test
    void generatedNumber_hasExpectedFormat() {
        String number = factory.generate(AccountType.CHECKING);
        assertThat(number).matches("CHK-\\d{10}");
    }

    @ParameterizedTest
    @EnumSource(AccountType.class)
    void generate_neverReturnsNull(AccountType type) {
        assertThat(factory.generate(type)).isNotNull();
    }

    @Test
    void consecutiveCalls_produceUniqueNumbers() {
        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            numbers.add(factory.generate(AccountType.CHECKING));
        }
        assertThat(numbers).hasSize(100);
    }
}
