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
| API Gateway | http://localhost:8080 | All API calls go here |
| Account Service | http://localhost:8081 | Direct access (dev only) |
| Transaction Service | http://localhost:8082 | Direct access (dev only) |
| Notification Service | http://localhost:8083 | Direct access (dev only) |
| Loan Service | http://localhost:8084 | Direct access (dev only) |
| AI Layer | http://localhost:8000 | Python FastAPI |
| Eureka Dashboard | http://localhost:8761 | Service registry |
| Config Server | http://localhost:8888 | Config endpoint |
| Kafka UI | http://localhost:9090 | Topic browser |
| ActiveMQ Admin | http://localhost:8161 | Queue browser (admin/admin) |

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
http://localhost:8161/console
admin / admin
Queues → nexabank.transaction.events → Number of Pending Messages
```
# nexabank
