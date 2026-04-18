package com.nexabank.account.integration;

import com.nexabank.account.dto.AuthResponse;
import com.nexabank.account.dto.RegisterRequest;
import com.nexabank.account.repository.AccountRepository;
import com.nexabank.account.repository.CustomerRepository;
import com.nexabank.account.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
@Transactional
class AccountServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;

    @MockBean private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void cleanUp() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void register_persistsCustomerAndCheckingAccount() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("integration@nexabank.com");
        req.setPassword("IntPass123!");
        req.setFirstName("Integration");
        req.setLastName("Test");
        req.setPhone("555-9999");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isNotBlank();
        assertThat(customerRepository.existsByEmail("integration@nexabank.com")).isTrue();
        assertThat(accountRepository.findByCustomerId(response.getCustomerId())).hasSize(1);
    }

    @Test
    void register_checkingAccountNumber_hasChkPrefix() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("prefix@nexabank.com");
        req.setPassword("IntPass123!");
        req.setFirstName("Prefix");
        req.setLastName("Test");
        req.setPhone("555-8888");

        AuthResponse response = authService.register(req);

        var accounts = accountRepository.findByCustomerId(response.getCustomerId());
        assertThat(accounts.get(0).getAccountNumber()).startsWith("CHK-");
    }

    @Test
    void register_duplicateEmail_rollsBackCustomer() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@nexabank.com");
        req.setPassword("IntPass123!");
        req.setFirstName("Dup");
        req.setLastName("Test");
        req.setPhone("555-7777");

        authService.register(req);

        try {
            authService.register(req);
        } catch (Exception ignored) {}

        assertThat(customerRepository.findAll()).hasSize(1);
    }
}
