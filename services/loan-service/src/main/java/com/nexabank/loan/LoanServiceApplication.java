package com.nexabank.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * NexaBank Loan Service
 *
 * Key patterns demonstrated:
 * - Strategy Pattern: LoanEligibilityStrategy — AI vs rule-based scoring
 * - Spring Boot 3 RestClient (replaces RestTemplate for HTTP calls)
 * - Feign client to account-service for account validation
 * - Kafka producer for loan status change events
 *
 * The AI scoring delegates to the Python FastAPI ai-layer,
 * extending the existing LoanSearch/AI integration concept from
 * the SpringBoot-Java-Angular-Python project.
 *
 * See docs/learning/11-design-patterns-used.md (Strategy section)
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
