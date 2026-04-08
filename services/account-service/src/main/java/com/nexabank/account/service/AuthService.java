package com.nexabank.account.service;

import com.nexabank.account.dto.AuthResponse;
import com.nexabank.account.dto.LoginRequest;
import com.nexabank.account.dto.RegisterRequest;
import com.nexabank.account.event.AccountCreatedEvent;
import com.nexabank.account.exception.DuplicateAccountException;
import com.nexabank.account.model.*;
import com.nexabank.account.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Authentication Service — registration and login.
 *
 * On register:
 * 1. Validates email uniqueness
 * 2. Hashes password with BCrypt
 * 3. Persists Customer + default CHECKING Account (in one transaction)
 * 4. Publishes AccountCreatedEvent to Kafka
 * 5. Returns JWT
 *
 * @Transactional ensures that if Kafka publish fails, the DB write is also rolled back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CustomerRepository customerRepository;
    private final AccountNumberFactory accountNumberFactory;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateAccountException(
                    "Email already registered: " + request.getEmail());
        }

        // Create customer
        Customer customer = Customer.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(CustomerStatus.ACTIVE)
                .build();

        // Save customer first to get the generated ID, then create account with number
        Customer saved = customerRepository.save(customer);

        Account checkingAccount = Account.builder()
                .accountNumber(accountNumberFactory.generate(AccountType.CHECKING))
                .accountType(AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .customer(saved)
                .build();

        saved.getAccounts().add(checkingAccount);
        saved = customerRepository.save(saved);
        Account account = saved.getAccounts().get(0);

        // Publish to Kafka — notification-service will send welcome email
        AccountCreatedEvent event = new AccountCreatedEvent(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName() + " " + saved.getLastName(),
                account.getId(),
                account.getAccountNumber(),
                AccountType.CHECKING.name(),
                Instant.now()
        );
        kafkaTemplate.send("nexabank.account.created", saved.getEmail(), event);
        log.info("Account created for customer {} — event published to Kafka", saved.getEmail());

        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(jwtService.getExpirationInstant())
                .customerId(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFirstName() + " " + saved.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(customer.getId(), customer.getEmail());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(jwtService.getExpirationInstant())
                .customerId(customer.getId())
                .email(customer.getEmail())
                .fullName(customer.getFirstName() + " " + customer.getLastName())
                .build();
    }
}
