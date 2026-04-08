# Spring Boot Microservices — NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What Is a Microservice?

A microservice is an independently deployable application that owns a single business capability. Instead of one large monolith, NexaBank runs as five separate Spring Boot applications, each with its own database schema, its own deployment lifecycle, and its own team ownership.

```
Monolith (what we avoid)          Microservices (NexaBank)
─────────────────────────         ─────────────────────────
One JAR file                      account-service.jar
One database schema               transaction-service.jar
Deploy everything together        notification-service.jar
One team owns everything          loan-service.jar
Scale entire app for 1 feature    api-gateway.jar (+ eureka + config)
```

## The Four Business Services

| Service | Port | Owns | Key Dependency |
|---|---|---|---|
| account-service | 8081 | Customers, Accounts, JWT issuance | PostgreSQL (nexabank_accounts) |
| transaction-service | 8082 | Deposits, Withdrawals, Transfers, Ledger | PostgreSQL + Kafka + ActiveMQ |
| notification-service | 8083 | Notification records, read/unread state | PostgreSQL + Kafka consumer + JMS consumer |
| loan-service | 8084 | Loan applications, AI scoring | PostgreSQL + Feign → ai-layer |

Each service has:
- Its own `pom.xml` (child of parent)
- Its own `Dockerfile`
- Its own database schema
- Its own Flyway migration scripts
- Its own `application.yml` (fetched from Config Server at startup)

## Standard Spring Boot Layers

Every NexaBank service follows this exact structure:

```
src/main/java/com/nexabank/{service}/
├── {Service}Application.java       ← @SpringBootApplication entry point
├── controller/                     ← HTTP layer (@RestController)
├── service/                        ← Business logic (@Service @Transactional)
├── repository/                     ← Data access (JpaRepository)
├── model/                          ← JPA entities (@Entity)
├── dto/                            ← Data transfer objects (request/response)
├── mapper/                         ← MapStruct DTO ↔ Entity converters
├── event/                          ← Kafka/MQ event payloads (Java records)
├── exception/                      ← Custom exceptions + GlobalExceptionHandler
└── config/                         ← Spring configuration classes
```

### Why This Matters for Interviews
This layered architecture enforces **separation of concerns** — the same principle behind MVC (Model-View-Controller). Controllers never touch the database. Repositories never contain business logic. This makes code testable and maintainable.

## Key Annotations — Quick Reference

| Annotation | Where Used | What It Does |
|---|---|---|
| `@SpringBootApplication` | Application class | Combines @Configuration + @EnableAutoConfiguration + @ComponentScan |
| `@RestController` | Controllers | Combines @Controller + @ResponseBody (returns JSON) |
| `@Service` | Service classes | Marks as Spring bean, enables AOP (e.g., @Transactional) |
| `@Repository` | Repository interfaces | Marks as data access layer, translates DB exceptions |
| `@Entity` | Model classes | Maps Java class to database table |
| `@Transactional` | Service methods | Wraps method in a database transaction |

## Example: account-service Request Flow

```
POST /api/accounts  (from Angular or API Gateway)
  │
  ↓ AccountController.java
  @PostMapping("/api/accounts")
  public ResponseEntity<AccountResponse> createAccount(
      @RequestBody @Valid AccountRequest request) {
      return ResponseEntity.status(201).body(accountService.create(request));
  }
  │
  ↓ AccountService.java
  @Transactional
  public AccountResponse create(AccountRequest request) {
      Account account = mapper.toEntity(request);
      account.setAccountNumber(accountNumberFactory.generate(request.getType()));
      Account saved = repository.save(account);           // DB write
      kafkaTemplate.send("nexabank.account.created", ...); // Kafka event
      return mapper.toResponse(saved);
  }
  │
  ↓ AccountRepository.java (JpaRepository<Account, Long>)
  // Spring Data generates SQL: INSERT INTO ACCOUNTS ...
  │
  ↓ PostgreSQL (nexabank_accounts schema)
```

## DTO Pattern — Why Not Return Entities Directly?

NexaBank uses separate `*Request` and `*Response` DTOs for every endpoint:

```java
// AccountRequest — what the client sends
public record AccountRequest(
    @NotNull Long customerId,
    @NotNull AccountType accountType,
    @DecimalMin("0.0") BigDecimal initialDeposit
) {}

// AccountResponse — what the client receives
public record AccountResponse(
    Long id,
    String accountNumber,
    AccountType accountType,
    BigDecimal balance,
    AccountStatus status
) {}
```

**Why:** The entity has password hashes, audit columns, and other fields you never want to expose. DTOs are the API contract — the entity is internal implementation.

## MapStruct — Zero-Boilerplate Mapping

Instead of writing `response.setId(entity.getId())` for every field:

```java
@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toResponse(Account account);
    Account toEntity(AccountRequest request);
}
```

MapStruct generates the implementation at compile time. 100% type-safe, no reflection overhead.

## @Transactional — What It Actually Does

```java
// Without @Transactional — PROBLEM:
// If Kafka publish fails after DB save, you have a saved transaction with no notification
public void transfer(TransferRequest req) {
    transactionRepo.save(txn);          // DB write succeeds
    kafkaTemplate.send("topic", event); // FAILS — but DB already committed!
}

// With @Transactional — SOLUTION:
// If anything throws, the entire method rolls back
@Transactional
public void transfer(TransferRequest req) {
    transactionRepo.save(txn);          // Queued in transaction
    kafkaTemplate.send("topic", event); // If this throws, DB rolls back too
}
```

**Banking context:** A transfer that debits one account but doesn't credit another is a serious bug. `@Transactional` prevents split-brain states.

## Spring Boot Auto-Configuration

When you add `spring-boot-starter-data-jpa` to `pom.xml`, Spring Boot:
1. Detects Hibernate on the classpath
2. Creates a `DataSource` bean from `application.yml` properties
3. Creates `EntityManagerFactory` and `TransactionManager` beans
4. Enables `@Repository` scanning

You write **zero boilerplate configuration**. This is the core value of Spring Boot.

## Flyway — Database Version Control

NexaBank uses Flyway to version-control schema changes:

```
services/account-service/src/main/resources/db/migration/
├── V1__create_account_tables.sql   ← Runs once on first startup
├── V2__add_account_tier.sql        ← Runs on next deployment (example)
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create tables — trust Flyway
```

**Why this matters in banking:** Schema changes in production need to be audited, repeatable, and reversible. `ddl-auto: create-drop` would destroy production data on restart.

## Application Startup Order

```
1. PostgreSQL (health check: pg_isready)
2. Zookeeper → Kafka (health check: kafka-topics.sh --list)
3. eureka-server (health check: /actuator/health)
4. config-server (depends on: eureka-server)
5. api-gateway (depends on: eureka-server, config-server)
6. account-service (depends on: config-server, postgres, kafka)
7. transaction-service (depends on: account-service, kafka, activemq)
8. notification-service (depends on: kafka, activemq)
9. loan-service (depends on: account-service, ai-layer)
10. nexabank-ui (Angular, depends on: api-gateway)
```

Docker Compose `depends_on` with health checks enforces this order.

## Interview Talking Points
- **What is the difference between `@Component`, `@Service`, `@Repository`, `@Controller`?** All register a Spring bean. `@Service` and `@Repository` are semantic aliases for `@Component`. `@Repository` additionally translates DB-specific exceptions. `@Controller` marks HTTP request handlers.
- **Why use microservices over a monolith?** Independent deployability, technology flexibility, fault isolation, independent scalability. Tradeoff: distributed system complexity (network calls, eventual consistency, distributed transactions).
- **What is Spring Boot auto-configuration?** Conditional bean creation based on classpath presence and property values. Eliminates XML configuration. Override with your own `@Bean` definitions.
- **What is a `@Transactional` rollback?** By default, Spring rolls back on unchecked exceptions (RuntimeException). For checked exceptions you must use `@Transactional(rollbackFor = Exception.class)`.

## Questions to Ask Your AI
- "What is the difference between Spring Boot and Spring Framework?"
- "How does Spring Boot auto-configuration work internally?"
- "What is the N+1 query problem in JPA and how do you fix it with JOIN FETCH?"
- "What is the difference between EAGER and LAZY loading in JPA?"
- "How would you test a Spring Boot service layer without starting the full application context?"
- "What is Spring Data JPA and how does it generate SQL from method names?"
