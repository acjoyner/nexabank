package com.nexabank.transaction.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feign client — calls account-service for balance checks.
 *
 * The 'name' attribute matches the spring.application.name of account-service.
 * Eureka resolves "account-service" to the actual host:port at runtime.
 *
 * Fallback: if account-service is unreachable (circuit open), returns
 * AccountServiceClientFallback.getBalance() — prevents cascading failure.
 *
 * See docs/learning/08-feign-circuit-breaker.md
 */
@FeignClient(
    name = "account-service",
    fallback = AccountServiceClient.AccountServiceClientFallback.class
)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable Long accountId);

    @PutMapping("/api/accounts/{accountId}/balance")
    BalanceResponse updateBalance(@PathVariable Long accountId,
                                  @RequestBody Map<String, BigDecimal> body);

    /**
     * DTO matching account-service's BalanceResponse — must stay in sync.
     * In a production system this would be a shared library artifact.
     */
    @Data
    @Builder
    class BalanceResponse {
        private Long accountId;
        private String accountNumber;
        private BigDecimal balance;
        private String status;

        public static BalanceResponse unavailable() {
            return BalanceResponse.builder()
                    .balance(BigDecimal.ZERO)
                    .status("FROZEN")
                    .build();
        }
    }

    /**
     * Circuit breaker fallback — returns a "safe" frozen/zero response
     * when account-service is unavailable. The transaction service will
     * then reject the transaction rather than proceeding blindly.
     */
    @Component
    @Slf4j
    class AccountServiceClientFallback implements AccountServiceClient {
        @Override
        public BalanceResponse getBalance(Long accountId) {
            log.warn("account-service unavailable — returning fallback for account {}", accountId);
            return BalanceResponse.unavailable();
        }

        @Override
        public BalanceResponse updateBalance(Long accountId, Map<String, BigDecimal> body) {
            log.warn("account-service unavailable — balance update skipped for account {}", accountId);
            return BalanceResponse.unavailable();
        }
    }
}
