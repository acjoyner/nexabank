# ADR-001: Saga Pattern for Distributed Transfer Transactions

**Status:** Accepted  
**Date:** 2024-11-01  
**Deciders:** Architecture Lead, Transaction Squad Lead  
**Ticket:** NEXA-89

---

## Context

The `transfer` operation in `transaction-service` touches multiple systems:
1. Reads account balance from `account-service` (Feign HTTP call)
2. Writes `TRANSACTIONS` record in the transaction-service database
3. Writes two `LEDGER_ENTRIES` records in the transaction-service database
4. Publishes a `TransactionCompletedEvent` to Kafka (`nexabank.transaction.completed`)
5. Publishes the same event to ActiveMQ (`nexabank.transaction.events`)

A traditional ACID transaction cannot span across a Feign HTTP call or a Kafka publish — these are network operations outside the database transaction scope. This raises the question: **how do we guarantee consistency across these steps when any one of them can fail?**

Three options were considered:

### Option A: Two-Phase Commit (2PC / XA Transaction)
Use a distributed transaction coordinator (e.g., Atomikos, Narayana) to lock all participating resources until all steps commit.

**Rejected because:**
- Not supported by Kafka (Kafka transactions exist but are complex and have performance overhead)
- XA transactions create distributed locks that block all participants — catastrophic at high volume
- Spring Boot + Kafka + JMS XA adds significant operational complexity
- Anti-pattern for microservices: tight coupling at the data layer defeats the purpose of independent services

### Option B: Outbox Pattern with Polling Publisher
Write the event to an `OUTBOX` table in the same local DB transaction as the `TRANSACTIONS` write. A separate polling process reads the outbox and publishes to Kafka/ActiveMQ, then deletes the outbox record.

**Not chosen for initial implementation because:**
- Adds an `OUTBOX` table and a polling publisher process to maintain
- Increases complexity for the initial MVP
- Appropriate for production hardening (see ADR-002)

### Option C: Saga Pattern (Chosen)
Break the operation into sequential steps, each with a compensating action. Use Spring's `@Transactional` to atomically handle the local database steps. Accept that the Kafka/JMS publish is "at most once" in the initial implementation.

---

## Decision

Implement the transfer as a **Choreography-based Saga** with local transaction rollback via `@Transactional`.

```
Transfer Saga Steps:
  1. Validate source account balance (Feign → account-service)
     Compensating action: none (read-only)

  2. BEGIN local @Transactional
     a. Persist TRANSACTIONS row (status=PENDING)
     b. Persist two LEDGER_ENTRIES rows (DEBIT + CREDIT)
     c. Publish to Kafka (nexabank.transaction.completed)
     d. Publish to ActiveMQ (nexabank.transaction.events)
     e. Update TRANSACTIONS row status → COMPLETED
     If any of a–e throws → Spring rolls back a, b, e atomically
     NOTE: Kafka and ActiveMQ publishes (c, d) are NOT rolled back by the DB transaction
  3. END @Transactional — commit
```

**Accepted limitation:** If the DB commit succeeds but the process crashes before Kafka/JMS publish, the event is lost. This is acceptable for the MVP demo application. For production hardening, ADR-002 (Outbox Pattern) resolves this gap.

---

## Consequences

**Positive:**
- Simple to implement with `@Transactional` — no distributed transaction coordinator
- Matches how most banking microservices handle this in the real world at the service-layer level
- The compensating rollback for DB steps is automatic (Spring handles it)
- Clear, readable `TransactionService.transfer()` method

**Negative:**
- Risk of event being published before the DB transaction commits (if Kafka publish succeeds but DB commit fails, a ghost event exists in Kafka)
- In the current implementation, Kafka/ActiveMQ publish happens inside the `@Transactional` boundary — this means if the DB commit rolls back, the Kafka message may already be in flight
- This "at least once / at most once" ambiguity is the exact problem the Outbox pattern solves

**Mitigations in place:**
- `CORRELATION_ID` column on `NOTIFICATIONS` table — allows idempotent processing (if a duplicate event arrives, check by `CORRELATION_ID` before inserting)
- `REFERENCE_NUMBER UNIQUE` constraint on `TRANSACTIONS` — prevents duplicate transaction records

---

## Review Trigger

This ADR should be revisited when:
- Transaction volume exceeds 100 TPS in any environment
- A duplicate notification incident is reported in production
- The Outbox pattern (ADR-002) is implemented
