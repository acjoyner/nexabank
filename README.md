# NexaBank Platform

A full-stack retail banking microservices application demonstrating senior banking tech lead concepts: Spring Boot/Cloud, Kafka, ActiveMQ (MQ), NDM/Connect:Direct simulation, Oracle-compatible SQL, JWT security, CI/CD with Jenkins and GitHub Actions, and an Angular frontend.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Angular UI (Port 4200)                      │
│              (Standalone Components, Angular Material)          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP /api/*
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              API Gateway  (Port 8080)                           │
│     JWT Validation · Rate Limiting · Request Correlation ID     │
│     Routes: /api/auth (public) · /api/** (JWT required)         │
└────┬──────────────┬──────────────┬─────────────────┬───────────┘
     │              │              │                 │
     ▼              ▼              ▼                 ▼
┌─────────┐  ┌──────────────┐ ┌──────────────┐ ┌──────────┐
│ account │  │ transaction  │ │notification  │ │  loan    │
│ service │  │   service    │ │   service    │ │ service  │
│  :8081  │  │    :8082     │ │    :8083     │ │  :8084   │
│         │  │              │ │              │ │          │
│ Customers│ │ Deposits     │ │ Kafka        │ │ Loan     │
│ Accounts │ │ Withdrawals  │ │ Consumer     │ │ Apply    │
│ JWT Auth │ │ Transfers    │ │ JMS Consumer │ │ AI Score │
└────┬────┘  └──────┬───────┘ └──────┬───────┘ └────┬─────┘
     │              │                │              │
     │    ┌─────────┴────────┐       │              │
     │    │  Apache Kafka    │◄──────┘              │
     │    │  (Port 9092)     │                      │
     │    │ nexabank.*topics │                      │
     │    └──────────────────┘                      │
     │                                              │
     │    ┌──────────────────┐                      │
     │    │  ActiveMQ Artemis│                      │
     │    │  (Port 61616)    │                      │
     │    │ nexabank.txn.evts│                      │
     │    └──────────────────┘                      │
     │                                              │
     ▼                                              ▼
┌──────────────────┐                    ┌──────────────────────┐
│   PostgreSQL     │                    │  Python FastAPI      │
│   (Port 5432)    │                    │  AI Layer (:8000)    │
│                  │                    │  /api/loan/eligibility│
│  nexabank_accts  │                    │  Rule-based ML scorer │
│  nexabank_txns   │                    └──────────────────────┘
│  nexabank_notifs │
│  nexabank_loans  │
└──────────────────┘

Support Services:
  Eureka Server   :8761  — Service discovery
  Config Server   :8888  — Centralized YAML config
  Kafka UI        :9090  — Topic browser
  ActiveMQ Admin  :8161  — Queue browser
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4 |
| Service Mesh | Spring Cloud 2023.0.3 (Eureka, Config Server, API Gateway, OpenFeign) |
| Event Streaming | Apache Kafka (Confluent 7.6.1), 6-partition topics |
| Message Queue | ActiveMQ Artemis 2.32.0, JMS point-to-point |
| Batch/File Transfer | NDM (Connect:Direct) simulation — `@Scheduled` + flat file |
| Database | PostgreSQL 16 with Oracle-compatible DDL (sequences, NUMERIC(19,4)) |
| Schema Migrations | Flyway |
| Security | Spring Security 6, JJWT 0.12.5, BCrypt |
| DTO Mapping | MapStruct 1.5.5 |
| API Docs | SpringDoc OpenAPI 2.5.0 (Swagger UI per service) |
| AI Layer | Python 3.11, FastAPI, Pydantic |
| Frontend | Angular 17, standalone components, Angular Material, RxJS |
| Containerization | Docker, Docker Compose |
| CI/CD | Jenkins (Declarative Pipeline) + GitHub Actions (3 workflows) |
| Testing | JUnit 5, Mockito, Testcontainers |

---

## Quick Start

### Prerequisites
- Docker Desktop (4GB memory minimum)
- Java 17+ (for local Maven builds)
- Node.js 20+ (for local Angular builds)

### 1. Clone and start all services

```bash
git clone <repo-url>
cd nexabank-platform
docker-compose up -d
```

Docker Compose starts services in dependency order with health checks. Allow ~2 minutes for all services to become healthy.

### 2. Verify startup

```bash
docker-compose ps
# All services should show "healthy" or "running"
```

Check the Eureka dashboard — all 4 business services should be registered:
```
http://localhost:8761
```

### 3. Register a user and get a JWT

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.doe@nexabank.com",
    "password": "SecurePass123!",
    "firstName": "Jane",
    "lastName": "Doe"
  }'
```

Response includes a `token` field. Copy it.

### 4. Open the Angular UI

```
http://localhost:4200
```

Log in with the credentials above. The dashboard shows account balance, recent transactions, and notifications.

---

## Service Ports

| Service | URL | Notes |
|---|---|---|
| Angular UI | http://localhost:4200 | Frontend |
| API Gateway | http://localhost:8080/actuator/health | All API calls go here |
| Account Service | http://localhost:8081/swagger-ui/index.html | Swagger UI (dev only) |
| Transaction Service | http://localhost:8082/swagger-ui/index.html | Swagger UI (dev only) |
| Notification Service | http://localhost:8083/swagger-ui/index.html | Swagger UI (dev only) |
| Loan Service | http://localhost:8084/swagger-ui/index.html | Swagger UI (dev only) |
| AI Layer | http://localhost:8000/docs | Python FastAPI Swagger |
| Eureka Dashboard | http://localhost:8761 | Service registry UI |
| Config Server | http://localhost:8888/application/default | Default config — use `/{service-name}/default` per service |
| Kafka UI | http://localhost:9090/ui/clusters/nexabank-cluster/consumer-groups | Consumer groups view |
| ActiveMQ Admin | http://localhost:8161/console/auth/login | Queue browser (admin/admin) |

---

## API Reference

### Authentication

```
POST /api/auth/register    — Create account + JWT
POST /api/auth/login       — Login + JWT
```

### Accounts

```
GET  /api/accounts                  — List accounts for authenticated user
GET  /api/accounts/{id}/balance     — Get current balance
POST /api/accounts                  — Create additional account
```

### Transactions

```
POST /api/transactions/deposit      — Deposit funds
POST /api/transactions/withdraw     — Withdraw funds
POST /api/transactions/transfer     — Transfer between accounts
GET  /api/transactions/account/{id} — Get transaction history
```

### Notifications

```
GET   /api/notifications/{customerId}  — List notifications
PATCH /api/notifications/{id}/read    — Mark as read
```

### Loans

```
POST  /api/loans/apply                      — Submit loan application
GET   /api/loans/{id}                       — Get loan status
GET   /api/loans/customer/{customerId}      — List customer loans
PATCH /api/loans/{id}/status               — Update status (admin)
```

All endpoints (except `/api/auth/*`) require `Authorization: Bearer <token>` header.

Swagger UI available per service: `http://localhost:{port}/swagger-ui.html`

---

## End-to-End Verification Checklist

```
□ 1. docker-compose up -d — all containers healthy
□ 2. POST /api/auth/register → JWT returned
□ 3. POST /api/transactions/transfer → transaction + ledger entries in DB
□ 4. Kafka UI at localhost:9090 → nexabank.transaction.completed topic has messages
□ 5. ActiveMQ admin at localhost:8161 → nexabank.transaction.events queue has messages
□ 6. GET /api/notifications/{customerId} → notification record created from Kafka consumer
□ 7. target/ndm-outbound/ → batch file generated after NDM scheduler fires (1:00am)
     (trigger manually: POST /api/transactions — then check at scheduled time, or reduce cron for testing)
□ 8. POST /api/loans/apply → aiDecision + aiReason populated from Python AI layer
□ 9. Angular at localhost:4200 → full login → dashboard → transfer → notification flow
□ 10. Eureka dashboard → 4 services registered (account, transaction, notification, loan)
```

---

## Local Development (without Docker)

### Run a single service

```bash
# Start only infrastructure
docker-compose up -d postgres zookeeper kafka activemq eureka-server config-server

# Build and run account-service locally
cd services/account-service
mvn spring-boot:run
```

### Run the AI Layer

**Option 1 — Docker (recommended, no Python install needed)**

```bash
# From the project root — starts everything including the AI layer
docker-compose up -d

# AI layer will be available at http://localhost:8000/docs
```

**Option 2 — uv (local dev, hot reload)**

```bash
cd ai-layer

# Install uv (if not installed)
curl -LsSf https://astral.sh/uv/install.sh | sh

# Create virtual environment and install dependencies
uv venv
uv pip install -e .

# Start with hot reload (http://localhost:8000)
uv run uvicorn main:app --reload --port 8000
```

### Run tests

```bash
# Unit tests only
mvn test

# Full build with integration tests (requires Docker for Testcontainers)
mvn verify -P integration-test
```

### Build Docker images

```bash
mvn package -DskipTests
docker-compose build
```

---

## Project Structure

```
nexabank-platform/
├── pom.xml                                    Parent POM
├── docker-compose.yml
├── Jenkinsfile                                Jenkins CI/CD pipeline
├── .github/workflows/
│   ├── ci.yml                                 PR validation
│   ├── cd-dev.yml                             Auto-deploy on develop push
│   └── cd-staging.yml                         Manual approval staging deploy
├── infrastructure/
│   ├── eureka-server/
│   ├── config-server/
│   └── api-gateway/
├── services/
│   ├── account-service/                       Customers, Accounts, JWT
│   ├── transaction-service/                   Deposits, Transfers, NDM, Ledger
│   ├── notification-service/                  Kafka + JMS consumer
│   └── loan-service/                          Loan applications, AI scoring
├── ai-layer/                                  Python FastAPI ML scorer
├── frontend/nexabank-ui/                      Angular 17 frontend
└── docs/
    ├── architecture-decision-records/         ADR-001 Saga, ADR-002 Outbox
    ├── offshore-onshore-guide.md              Team collaboration guide
    ├── api-contract.yaml                      OpenAPI 3.0 contract
    └── learning/                              Markdown files for Ollama/Open WebUI
        ├── 00-project-overview.md
        ├── 01-spring-boot-microservices.md
        ├── 02-spring-cloud-eureka-gateway.md
        ├── 03-kafka-event-streaming.md
        ├── 04-activemq-mq-messaging.md
        ├── 05-ndm-batch-file-transfer.md
        ├── 06-oracle-sql-schema.md
        ├── 07-jwt-spring-security.md
        ├── 08-feign-circuit-breaker.md
        ├── 09-angular-standalone-components.md
        ├── 10-ci-cd-jenkins-github-actions.md
        └── 11-design-patterns-used.md
```

---

## Test Strategy

### Coverage Summary

| Layer | Framework | Status |
|---|---|---|
| API Gateway | JUnit 5 + reactor-test | ✅ JWT filter, logging filter, fallback controller |
| Eureka Server | JUnit 5 + @SpringBootTest | ✅ Context smoke test |
| Config Server | JUnit 5 + @SpringBootTest | ✅ Context smoke test |
| Account Service | JUnit 5 + Mockito + MockMvc | ✅ Auth, Account, JWT, factory, controllers |
| Transaction Service | JUnit 5 + Mockito | ✅ Ledger, transactions, deposit/withdrawal/transfer |
| Notification Service | JUnit 5 + Mockito | ✅ Service, Kafka handler, JMS handler |
| Loan Service | JUnit 5 + Mockito | ✅ Rule strategy, AI strategy, service, controller |
| AI Layer | pytest + httpx TestClient | ✅ Scorer logic, all API endpoints |
| Angular | Karma + Jasmine | ✅ Auth service, dashboard, deposit, withdrawal, transfer, loan apply |

### Test Phases

**Phase 1 — Infrastructure**
- `api-gateway`: `JwtAuthFilterTest`, `RequestLoggingFilterTest`, `FallbackControllerTest`, smoke test
- `eureka-server` / `config-server`: `@SpringBootTest` context load tests

**Phase 2 — Business Services (unit tests with Mockito)**
- Service layer tests for account, transaction, notification, loan
- Controller tests with `MockMvc`
- Kafka listener tests with mocked `ObjectMapper`

**Phase 3 — Integration Tests (H2 + Spring context)**
- `AccountServiceIntegrationTest` — register flow, balance persistence
- `LoanEligibilityIntegrationTest` — rule-based strategy wired through Spring

**Phase 4 — AI Layer (uv + pytest)**
- `test_loan_scorer.py` — 18 tests covering all scoring rules, DTI, loan-to-income, edge cases
- `test_api.py` — 14 endpoint tests covering eligibility, policy-check, validation, docs

**Phase 5 — Angular (Karma + Jasmine)**
- `auth.service.spec.ts`, `dashboard.component.spec.ts`, `deposit.component.spec.ts`
- `withdrawal.component.spec.ts`, `transfer.component.spec.ts`, `loan-apply.component.spec.ts`

---

## Running Tests

### Maven (Java services)

```bash
# Run all unit tests across all services
mvn test

# Run single service
cd services/account-service && mvn test
cd services/transaction-service && mvn test
cd services/notification-service && mvn test
cd services/loan-service && mvn test
cd infrastructure/api-gateway && mvn test
cd infrastructure/eureka-server && mvn test
cd infrastructure/config-server && mvn test

# Run with integration tests
mvn verify

# Generate JaCoCo coverage report (per service)
mvn jacoco:report
```

### Angular

```bash
cd frontend/nexabank-ui

# Run tests (interactive, with watch)
npm test

# Run tests once (CI mode, headless)
npm test -- --watch=false --browsers=ChromeHeadless

# Run with coverage
npm test -- --code-coverage --watch=false --browsers=ChromeHeadless
```

### AI Layer (uv + pytest)

```bash
cd ai-layer

# Install uv (if not installed)
curl -LsSf https://astral.sh/uv/install.sh | sh

# Create virtual environment and install dependencies
uv venv
uv pip install -e ".[test]"

# Run all tests
uv run pytest

# Run with verbose output
uv run pytest -v

# Run specific test file
uv run pytest tests/test_loan_scorer.py -v
uv run pytest tests/test_api.py -v

# Run the AI layer server
uv run uvicorn main:app --reload --port 8000
```

---

## Reports

All generated reports are written to the `reports/` directory at the project root.

| Report | Location | Generated by |
|---|---|---|
| JaCoCo (per service) | `{service}/target/site/jacoco/index.html` | `mvn test` or `mvn jacoco:report` |
| Angular HTML coverage | `reports/angular/index.html` | `npm test -- --code-coverage` |
| Angular lcov | `reports/angular/lcov.info` | `npm test -- --code-coverage` |
| pytest HTML report | `reports/pytest/report.html` | `uv run pytest` |
| pytest coverage HTML | `reports/pytest/coverage/index.html` | `uv run pytest` |

> **Note:** The `reports/` folder is git-ignored for generated content. Run the test commands above to regenerate locally.

---

## Design Patterns Implemented

| Pattern | Location | Purpose |
|---|---|---|
| Repository | `*Repository.java` (all services) | Decouple data access from business logic |
| Service Layer / Façade | `TransactionService.java` | Hide multi-system complexity behind one method |
| DTO + MapStruct | `*Mapper.java`, `dto/` (all services) | Prevent entity leakage, control API contract |
| Circuit Breaker | `AccountServiceClientFallback.java` | Resilience when account-service is unavailable |
| Event-Driven / Observer | `KafkaEventHandler.java`, `JmsEventHandler.java` | Async decoupling of transaction and notification |
| Strategy | `AiEligibilityStrategy.java` + `RuleBasedEligibilityStrategy.java` | Swappable loan scoring algorithms |
| Factory | `AccountNumberFactory.java` | Centralized account number generation |
| Saga | `TransactionService.transfer()` | Distributed transaction management |
| RFC 7807 Problem Detail | `GlobalExceptionHandler.java` (all services) | Standardized error response format |

---

## Key Concepts Covered

This application was designed to cover every concept commonly found in senior banking tech lead job descriptions:

- **Spring Boot microservices** — 4 business services + 3 infrastructure services
- **Spring Cloud** — Eureka (discovery), Config Server (centralized config), API Gateway (routing, auth, rate limiting), OpenFeign (declarative HTTP client), Resilience4j (circuit breaker)
- **Apache Kafka** — event streaming, partitioned topics, consumer groups, Java records as payloads
- **ActiveMQ / MQ** — JMS guaranteed delivery, point-to-point queues, `JmsTemplate` + `@JmsListener`
- **NDM / Connect:Direct** — batch file transfer simulation with `@Scheduled`, pipe-delimited flat files, HDR/DTL/TRL format, control totals
- **Oracle-compatible SQL** — sequences (not SERIAL), NUMERIC(19,4), uppercase names, named constraints, Flyway migrations
- **Double-entry bookkeeping** — every transaction creates two LEDGER_ENTRIES (DEBIT + CREDIT)
- **JWT Security** — JJWT 0.12.x, validated at gateway, identity forwarded via HTTP headers
- **Jenkins + GitHub Actions** — parallel quality stages, branch-conditional deployment, manual approval gates
- **Angular 17** — standalone components, functional interceptors, reactive forms, Angular Material
- **Offshore/onshore delivery** — service ownership matrix, branching conventions, escalation paths

---

## Tech Lead Runbook

### Service won't register with Eureka
```bash
docker logs nexabank-platform-eureka-server-1
# Check config-server is reachable from the service
docker exec nexabank-platform-account-service-1 wget -q -O- http://config-server:8888/account-service/default
```

### Kafka consumer not processing messages
```bash
# Check consumer group lag
docker exec nexabank-platform-kafka-1 \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group notification-group --describe
```

### Reset Kafka consumer group offset (dev only)
```bash
docker exec nexabank-platform-kafka-1 \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group notification-group \
  --topic nexabank.transaction.completed \
  --reset-offsets --to-earliest --execute
```

### Database access
```bash
docker exec -it nexabank-platform-postgres-1 psql -U nexabank -d nexabank_db
# Switch schema
SET search_path TO nexabank_transactions;
SELECT * FROM TRANSACTIONS ORDER BY CREATED_AT DESC LIMIT 10;
```

### Force NDM batch file generation (without waiting for 1am cron)
Connect to the transaction-service and trigger the NDM method directly via Actuator, or temporarily change the cron expression in `config/transaction-service.yml` to run every minute during testing.

### Check ActiveMQ queue depth
```
http://localhost:8161/console/auth/login
admin / admin
Queues → nexabank.transaction.events → Number of Pending Messages
```
# nexabank
