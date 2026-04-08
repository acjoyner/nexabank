# NexaBank Platform — Project Overview

> **Paste this file into Ollama/Open WebUI as system context** before asking questions about this project.

## What This Project Is
NexaBank is a retail banking microservices portfolio application built to demonstrate every skill on senior banking tech lead job descriptions. It is a real, runnable codebase — not pseudocode.

## Architecture Map

```
Angular UI (port 4200)
  └─> API Gateway (port 8080) — JWT validation, routing, CORS
        ├─> account-service  (8081) — customers, accounts, authentication
        ├─> transaction-service (8082) — deposits, withdrawals, transfers, ledger
        ├─> notification-service (8083) — event consumers (Kafka + ActiveMQ)
        └─> loan-service (8084) — applications, AI eligibility scoring
              └─> ai-layer Python FastAPI (8000) — mock ML scorer

Supporting Infrastructure:
  Eureka Server (8761)  — service discovery
  Config Server (8888)  — centralized YAML configuration
  Kafka (9092)          — event streaming
  ActiveMQ (61616)      — MQ guaranteed delivery
  PostgreSQL (5432)     — Oracle-compatible relational DB
  Kafka UI (9090)       — visualize topics/messages
  ActiveMQ Admin (8161) — MQ admin console
```

## Project Location
`/Users/anthonyjoyner/Documents/Projects/nexabank-platform/`

## Technology Stack Reference

| Technology | Version | Where Used |
|---|---|---|
| Spring Boot | 3.3.4 | All Java services |
| Spring Cloud | 2023.0.3 (Leyton) | Eureka, Gateway, Config, Feign |
| Java | 17 | All services |
| Apache Kafka | 7.6.1 (Confluent) | Event streaming |
| ActiveMQ Artemis | 2.32.0 | MQ messaging |
| PostgreSQL | 16 | Oracle-compatible DB (all services) |
| Flyway | (Boot-managed) | DB schema migrations |
| Angular | 17 | Frontend |
| FastAPI (Python) | 0.111.0 | AI loan scoring |
| Jenkins | Declarative Pipeline | CI/CD (Jenkinsfile) |
| GitHub Actions | — | Modern CI/CD (.github/workflows/) |
| Docker Compose | — | Local dev orchestration |

## Job Description Coverage

| JD Requirement | Implementation |
|---|---|
| Core Java, Spring MVC, Spring Boot | All 4 business services |
| Spring Cloud | Eureka + Config Server + API Gateway |
| Kafka | transaction-service produces, notification-service consumes |
| MQ (ActiveMQ) | transaction-service → notification-service via JMS |
| NDM (Connect:Direct) | NdmFileService.java — scheduled batch file generation |
| CI/CD Jenkins | Jenkinsfile with 8 stages |
| GitHub Actions | .github/workflows/ — CI, CD-dev, CD-staging |
| Oracle/SQL | Flyway migrations with Oracle-compatible DDL |
| JavaScript/Angular | nexabank-ui — standalone Angular 17 components |
| Agile/SDLC | Jenkinsfile pipeline stages, offshore-onshore-guide.md |
| Technical Architecture | ADRs in docs/architecture-decision-records/ |
| Onshore-Offshore | docs/offshore-onshore-guide.md |
| Team Leadership | RequestLoggingFilter correlation IDs, NDM + MQ patterns |

## How to Ask Me Questions

With this context loaded, you can ask me things like:
- "Explain how the JWT flows from Angular through the gateway to account-service"
- "Walk me through what happens when a transfer is submitted"
- "What is the difference between Kafka and ActiveMQ in this app?"
- "How does the NDM file transfer simulation work?"
- "Explain the Strategy pattern in the loan-service"
- "What does the Feign circuit breaker fallback do?"
