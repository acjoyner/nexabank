package com.nexabank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NexaBank Account Service
 *
 * Owns: customer registration/login, JWT issuance, bank account management.
 * All other services trust the JWT issued here — they do NOT have their own AuthController.
 *
 * Key patterns demonstrated:
 * - Spring Security + JWT (stateless authentication)
 * - Spring Data JPA with Oracle-compatible sequences
 * - Kafka producer (account.created events)
 * - MapStruct DTO mapping
 * - RFC 7807 ProblemDetail error responses
 *
 * See docs/learning/01-spring-boot-microservices.md
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
