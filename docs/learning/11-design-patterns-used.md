# Design Patterns in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## Why Design Patterns Matter in Banking Interviews

Senior banking tech lead job descriptions ask for "object-oriented design," "SOLID principles," and "proven architectural patterns." Design patterns are the vocabulary used to describe solutions that have been applied to common recurring problems. Knowing the name, intent, and trade-offs of each pattern — and where it appears in real code — is what separates senior candidates from mid-level.

NexaBank deliberately implements nine patterns across the codebase. This doc maps each to its exact file location.

---

## 1. Repository Pattern

**Intent:** Decouple business logic from data access. The service layer never writes SQL — it calls a repository method.

**File:** Every `*Repository.java` in each service.

```java
// services/account-service/src/main/java/.../repository/AccountRepository.java
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a JOIN FETCH a.customer WHERE a.customer.id = :customerId")
    List<Account> findByCustomerIdWithCustomer(@Param("customerId") Long customerId);
}
```

**Why in banking:** Regulatory audits often require changing the data store (e.g., PostgreSQL → Oracle) without rewriting business logic. The repository abstraction makes this a one-layer change.

**SOLID connection:** Dependency Inversion Principle — `AccountService` depends on the `AccountRepository` interface, not a concrete implementation.

---

## 2. Service Layer Pattern (also: Façade)

**Intent:** Encapsulate a subsystem's complexity behind a simple interface. Controllers don't know about Kafka, Flyway, or JPA — they call a service method.

**File:** Every `*Service.java` — most clearly shown in:
`services/transaction-service/src/main/java/.../service/TransactionService.java`

```java
@Transactional
public TransactionResponse transfer(TransferRequest request) {
    // 1. Validate via Feign client
    // 2. Persist Transaction entity
    // 3. Persist two LedgerEntries (double-entry bookkeeping)
    // 4. Publish to Kafka
    // 5. Publish to ActiveMQ
    // 6. Return mapped DTO
}
```

**Why in banking:** A transfer touches four systems (DB, Feign, Kafka, ActiveMQ). The controller calls one method — `transfer()`. This is the Façade pattern in practice.

---

## 3. DTO + Mapper Pattern

**Intent:** Separate the internal data model (JPA entity) from the external API contract (DTO). Prevent over-posting and under-exposing.

**Files:**
- `services/account-service/src/main/java/.../dto/AccountResponse.java`
- `services/account-service/src/main/java/.../mapper/AccountMapper.java`

```java
// Mapper — generated at compile time by MapStruct
@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toResponse(Account account);
    Account toEntity(AccountRequest request);
}
```

**Why in banking:** The `CUSTOMERS` table has a `PASSWORD_HASH` column. The `CustomerResponse` DTO never includes it. The mapper enforces this boundary at the type level — you can't accidentally expose it.

---

## 4. Circuit Breaker Pattern

**Intent:** Prevent cascading failures. When a downstream service is unresponsive, fail fast instead of queuing up thousands of waiting threads.

**File:** `services/transaction-service/src/main/java/.../client/AccountServiceClient.java`

```java
@FeignClient(name = "account-service", fallback = AccountServiceClientFallback.class)
public interface AccountServiceClient {
    @GetMapping("/api/accounts/{id}/balance")
    BalanceResponse getBalance(@PathVariable Long id);
}

// Fallback — returned when account-service circuit is OPEN
@Component
public class AccountServiceClientFallback implements AccountServiceClient {
    @Override
    public BalanceResponse getBalance(Long id) {
        return BalanceResponse.unavailable(); // balance=0, status=FROZEN
    }
}
```

**Three states of a circuit breaker:**
```
CLOSED → normal operation, requests pass through
OPEN   → failure threshold exceeded, requests fail immediately (no timeout wait)
HALF-OPEN → trial requests to check if service recovered
```

**Why in banking:** If account-service goes down, transaction-service should not queue 10,000 threads waiting for a timeout. It returns a graceful error immediately, logs the issue, and lets the circuit auto-reset after a configured wait.

---

## 5. Event-Driven / Observer Pattern

**Intent:** Decouple producers from consumers. The transaction-service doesn't know or care that notification-service exists — it just publishes an event.

**Producer:**
`services/transaction-service/src/main/java/.../service/TransactionService.java`
```java
kafkaTemplate.send("nexabank.transaction.completed", referenceNumber, event);
```

**Consumer:**
`services/notification-service/src/main/java/.../service/KafkaEventHandler.java`
```java
@KafkaListener(topics = "nexabank.transaction.completed", groupId = "notification-group")
public void handleTransactionCompleted(String message) {
    // Parse event, create Notification record
}
```

**Why in banking:** Transaction processing is a hot path — it must return in milliseconds. Notification generation, audit logging, and fraud scoring can happen asynchronously after the transaction completes. Event-driven architecture enables this decoupling.

---

## 6. Strategy Pattern

**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

**Files:**
- `services/loan-service/src/main/java/.../strategy/LoanEligibilityStrategy.java` (interface)
- `services/loan-service/src/main/java/.../strategy/AiEligibilityStrategy.java` (Python AI impl)
- `services/loan-service/src/main/java/.../strategy/RuleBasedEligibilityStrategy.java` (fallback impl)

```java
// Strategy interface
public interface LoanEligibilityStrategy {
    EligibilityResult evaluate(LoanApplicationRequest request);
}

// In LoanApplicationService — runtime strategy selection
private EligibilityResult evaluate(LoanApplicationRequest request) {
    try {
        return aiStrategy.evaluate(request);   // Try AI first
    } catch (RestClientException e) {
        log.warn("AI layer unavailable, falling back to rule-based strategy");
        return ruleStrategy.evaluate(request); // Fallback to rules
    }
}
```

**Why in banking:** Loan eligibility algorithms change frequently (regulatory updates, risk appetite changes). Swapping strategies requires no changes to the controller or repository — only a new `LoanEligibilityStrategy` implementation.

**SOLID connection:** Open/Closed Principle — the service is open for extension (new strategies) but closed for modification (existing code doesn't change).

---

## 7. Factory Pattern

**Intent:** Centralize object creation logic. Callers don't need to know how an account number is constructed.

**File:** `services/account-service/src/main/java/.../AccountNumberFactory.java`

```java
@Component
public class AccountNumberFactory {

    public String generate(AccountType type) {
        String prefix = switch (type) {
            case CHECKING -> "CHK";
            case SAVINGS  -> "SAV";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        // Example: CHK-A3F9B2C1D8E4
    }
}
```

**Why in banking:** Account number format is a business rule (prefix, length, check digit). It can change. Centralizing it in a factory means one class to update — not a regex scattered across 10 files.

---

## 8. Saga Pattern (Simplified)

**Intent:** Manage distributed transactions across microservices without a two-phase commit. Each step has a compensating transaction for rollback.

**File:** `services/transaction-service/src/main/java/.../service/TransactionService.java`

```
Transfer Saga:
  Step 1: Validate source account balance (via Feign → account-service)
  Step 2: Persist Transaction record (PENDING status)
  Step 3: Create two LedgerEntries (DEBIT + CREDIT)
  Step 4: Publish event to Kafka + ActiveMQ
  Step 5: Update Transaction status → COMPLETED

If Step 3 fails → @Transactional rolls back Steps 2+3
If Step 4 fails → @Transactional rolls back Steps 2+3+4
```

**The limitation in NexaBank:** `@Transactional` handles local DB rollback. It does NOT roll back the Feign call to account-service (network call already completed). A full Saga implementation would add compensating calls (e.g., `POST /api/accounts/{id}/unfreeze` if the transaction fails).

**Why in banking:** Distributed transactions are unavoidable in banking. The Saga pattern is the industry-standard approach to managing them without distributed locks (which don't scale).

---

## 9. RFC 7807 Problem Detail (Error Handling Pattern)

**Intent:** Standardize error responses so clients don't parse 10 different error formats.

**File:** Every `GlobalExceptionHandler.java` across all services.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage()
        );
        problem.setTitle("Account Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
```

**Wire format:**
```json
{
  "type": "about:blank",
  "title": "Account Not Found",
  "status": 404,
  "detail": "Account CHK-A3F9B2C1D8E4 does not exist",
  "timestamp": "2024-11-15T14:32:00Z"
}
```

**Why in banking:** Downstream services and Angular clients can write a single error-parsing function. Consistent error contracts are part of API governance.

---

## Pattern Summary Table

| Pattern | File(s) | Category |
|---|---|---|
| Repository | `*Repository.java` (all services) | Structural |
| Service Layer / Façade | `TransactionService.java` | Structural |
| DTO + Mapper | `*Mapper.java`, `dto/` (all services) | Structural |
| Circuit Breaker | `AccountServiceClientFallback.java` | Behavioral |
| Event-Driven / Observer | `KafkaEventHandler.java`, `JmsEventHandler.java` | Behavioral |
| Strategy | `AiEligibilityStrategy.java`, `RuleBasedEligibilityStrategy.java` | Behavioral |
| Factory | `AccountNumberFactory.java` | Creational |
| Saga | `TransactionService.transfer()` | Architectural |
| RFC 7807 Error | `GlobalExceptionHandler.java` (all services) | API Design |

---

## SOLID Principles Demonstrated

| Principle | Where |
|---|---|
| **S**ingle Responsibility | Each class has one job: `JwtService` only issues/validates tokens |
| **O**pen/Closed | `LoanEligibilityStrategy` — add new strategies without modifying existing code |
| **L**iskov Substitution | `AiEligibilityStrategy` and `RuleBasedEligibilityStrategy` are fully interchangeable |
| **I**nterface Segregation | `AccountServiceClient` (Feign) exposes only the methods that callers need |
| **D**ependency Inversion | Services depend on `*Repository` interfaces, not concrete implementations |

---

## Interview Talking Points
- **What design patterns have you used in a production banking system?** Name Circuit Breaker (resilience), Event-Driven (async decoupling), Saga (distributed transactions), and Repository (data abstraction). Tie each to a specific business reason.
- **What is the difference between Factory and Builder?** Factory creates objects of different types via a common interface. Builder constructs one complex object step-by-step (e.g., a `LoanApplicationRequest` with 12 fields where some are optional).
- **What is the Open/Closed Principle?** A class should be open for extension (add new behavior) but closed for modification (don't change working code). Strategy pattern is the textbook implementation.
- **How does the Circuit Breaker pattern improve system resilience?** It prevents thread exhaustion during downstream failures and provides graceful degradation. Without it, a single slow dependency can bring down the entire call stack through timeout accumulation.

## Questions to Ask Your AI
- "What is the difference between the Strategy pattern and the State pattern?"
- "How would you implement the Outbox pattern to guarantee at-least-once Kafka delivery in NexaBank?"
- "What is the Decorator pattern and how does Spring's `@Transactional` use it?"
- "How does the Hexagonal Architecture (Ports and Adapters) relate to the layered architecture in NexaBank?"
- "What is the difference between CQRS and Event Sourcing, and when would you use each in banking?"
