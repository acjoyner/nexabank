# Feign Client + Circuit Breaker in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What Is Feign?
Spring Cloud OpenFeign is a declarative HTTP client. Instead of writing `RestTemplate` calls manually, you define an interface with Spring MVC annotations and Spring generates the implementation.

```java
// Without Feign — verbose RestTemplate code:
ResponseEntity<BalanceResponse> response = restTemplate.exchange(
    "http://account-service/api/accounts/" + accountId + "/balance",
    HttpMethod.GET, null,
    new ParameterizedTypeReference<BalanceResponse>() {}
);

// With Feign — declare the interface:
@FeignClient(name = "account-service")
interface AccountServiceClient {
    @GetMapping("/api/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable Long accountId);
}
// Spring generates the implementation — just inject and call getBalance(123)
```

## What Is a Circuit Breaker?
Named after electrical circuit breakers that prevent overloading. In software:
- **Closed (normal)**: requests flow through
- **Open (failing)**: requests immediately go to fallback (no waiting for timeout)
- **Half-Open (testing)**: some requests allowed through to test if service recovered

**Why important for banking?** If account-service is down, transaction-service shouldn't queue 1000 requests waiting for it to respond. The circuit breaker detects failures and short-circuits immediately, returning a safe fallback.

## Key Code

### Feign Client with Fallback (transaction-service)
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/client/AccountServiceClient.java`

```java
@FeignClient(
    name = "account-service",                           // matches spring.application.name
    fallback = AccountServiceClientFallback.class       // called when circuit is open
)
public interface AccountServiceClient {
    @GetMapping("/api/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable Long accountId);
}
```

The `name = "account-service"` uses **Eureka service discovery** — Feign resolves the actual host:port from Eureka at runtime. No hardcoded URLs.

### Fallback Implementation
```java
@Component
class AccountServiceClientFallback implements AccountServiceClient {
    @Override
    public BalanceResponse getBalance(Long accountId) {
        log.warn("account-service unavailable — fallback triggered");
        return BalanceResponse.unavailable();  // balance=0, status=FROZEN
    }
}
```

The fallback returns `status=FROZEN` — `TransactionService.transfer()` then throws `AccountFrozenException`, which returns a proper error to the user rather than hanging.

### How the Circuit Breaker Fires
1. Normal: Feign calls `GET /api/accounts/{id}/balance` → success
2. 5 consecutive failures: Resilience4j opens the circuit
3. Open circuit: fallback called immediately (no HTTP call made)
4. After 30 seconds (half-open): Resilience4j tries one request
5. If it succeeds: circuit closes; if it fails: stays open

## Service Discovery Integration
**File:** `TransactionServiceApplication.java` has `@EnableFeignClients`

The `name = "account-service"` in `@FeignClient` is the `spring.application.name` registered with Eureka. Spring Cloud automatically does **load balancing** — if multiple instances of account-service are running, Feign distributes requests between them using round-robin.

## Interview Talking Points
- **Why Feign over RestTemplate?** Less boilerplate, built-in load balancing, easier fallback configuration. RestTemplate is being deprecated in newer Spring versions.
- **How does the circuit breaker prevent cascading failures?** Without circuit breaker: one slow service causes thread pool exhaustion in all callers. With circuit breaker: after threshold failures, calls fail fast and threads are freed.
- **What is the difference between retry and circuit breaker?** Retry tries the same call multiple times. Circuit breaker stops calling entirely. Use retry for transient failures (network blip), circuit breaker for service outages.
- **How does Feign find account-service?** Via Eureka — `lb://account-service` URL scheme. Load-balanced by Spring Cloud LoadBalancer.

## Questions to Ask Your AI
- "What are the three states of a circuit breaker?"
- "How would I add retry logic to the Feign client in addition to circuit breaker?"
- "What is the difference between @FeignClient fallback and fallbackFactory?"
- "How does Spring Cloud LoadBalancer work with Eureka?"
- "What metrics does Resilience4j expose for circuit breaker monitoring?"
