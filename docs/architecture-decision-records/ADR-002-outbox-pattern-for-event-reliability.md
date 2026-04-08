# ADR-002: Outbox Pattern for Guaranteed Event Delivery

**Status:** Proposed (not yet implemented — planned for Phase 2)  
**Date:** 2024-11-01  
**Deciders:** Architecture Lead  
**Ticket:** NEXA-91  
**Supersedes:** Partially supersedes the Kafka publish approach in ADR-001

---

## Context

ADR-001 acknowledged a known limitation: Kafka events published inside a `@Transactional` boundary can be "in flight" before the database commit completes. If the JVM crashes after Kafka receives the message but before the DB transaction commits (or vice versa), we get a split state:

**Scenario A — Ghost event:**
```
DB transaction ROLLS BACK
Kafka message ALREADY SENT ← notification-service creates a notification for a transaction that doesn't exist in the DB
```

**Scenario B — Missed event:**
```
DB transaction COMMITS
JVM crashes before kafkaTemplate.send() is called ← transaction exists in DB, no notification ever sent
```

In a retail banking context, Scenario B means a customer transfers money and receives no confirmation notification. This is a compliance and UX issue.

The **Transactional Outbox Pattern** is the industry-standard solution.

---

## Decision

When implemented, the Outbox Pattern will work as follows:

### Step 1: Write event to OUTBOX table in the same local transaction

```sql
-- New table to add in a future Flyway migration
CREATE TABLE OUTBOX_EVENTS (
    ID              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    AGGREGATE_TYPE  VARCHAR(100)    NOT NULL,  -- e.g., 'Transaction'
    AGGREGATE_ID    VARCHAR(255)    NOT NULL,  -- e.g., reference number
    EVENT_TYPE      VARCHAR(100)    NOT NULL,  -- e.g., 'TransactionCompletedEvent'
    PAYLOAD         TEXT            NOT NULL,  -- JSON
    CREATED_AT      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PUBLISHED_AT    TIMESTAMP       NULL       -- NULL = not yet published
);
```

```java
// TransactionService.transfer() — modified
@Transactional
public TransactionResponse transfer(TransferRequest request) {
    // ... existing logic to save Transaction + LedgerEntries ...

    // Instead of kafkaTemplate.send() here:
    outboxRepository.save(OutboxEvent.builder()
        .aggregateType("Transaction")
        .aggregateId(txn.getReferenceNumber())
        .eventType("TransactionCompletedEvent")
        .payload(objectMapper.writeValueAsString(event))
        .build());

    // Both TRANSACTIONS write and OUTBOX write commit atomically
    // If the JVM crashes here, the DB rolls back both — clean state
}
```

### Step 2: Polling Publisher reads OUTBOX and publishes to Kafka

```java
// New component: OutboxPublisher.java
@Scheduled(fixedDelay = 1000) // every 1 second
@Transactional
public void publishPending() {
    List<OutboxEvent> pending = outboxRepository.findByPublishedAtIsNull();
    for (OutboxEvent event : pending) {
        kafkaTemplate.send(topicFor(event.getEventType()), event.getAggregateId(), event.getPayload());
        event.setPublishedAt(Instant.now());
        outboxRepository.save(event);
    }
}
```

### Delivery Guarantee

```
Old approach (ADR-001):
  DB commit and Kafka publish are separate operations — split brain possible

Outbox approach (ADR-002):
  DB commit includes OUTBOX row — atomic
  Polling publisher reads OUTBOX rows and publishes to Kafka — at least once
  Consumer uses CORRELATION_ID for idempotency — effectively exactly once
```

---

## Why Not CDC (Change Data Capture)?

A more advanced implementation uses Debezium CDC to tail the PostgreSQL write-ahead log (WAL) instead of polling:

```
PostgreSQL WAL → Debezium → Kafka Connect → Kafka topic
```

**Advantages of CDC over polling:**
- Sub-millisecond latency (no polling interval)
- No polling query load on the database
- Truly decoupled from application code

**Why CDC is deferred:**
- Requires Debezium deployment (additional operational complexity)
- Requires PostgreSQL logical replication enabled (`wal_level=logical`)
- Not supported in all managed database services without configuration
- Polling publisher is sufficient for the current volume

CDC should be chosen when transaction volume exceeds ~1,000 events/second or when the polling query becomes a measurable DB load.

---

## Consequences

**Positive:**
- Eliminates split-brain between DB state and Kafka/ActiveMQ
- No message is ever lost — OUTBOX rows persist until published
- Simple recovery: restart the polling publisher, it picks up unpublished rows

**Negative:**
- Adds OUTBOX table and polling publisher component to maintain
- At-least-once delivery means consumers must be idempotent (already handled via CORRELATION_ID in notification-service)
- Polling latency: events appear in Kafka up to 1 second after the DB commit (acceptable for notifications, not for real-time fraud detection)

---

## Implementation Prerequisites

Before implementing, ensure:
1. `OUTBOX_EVENTS` Flyway migration is written and tested
2. `notification-service.KafkaEventHandler` is verified idempotent (check by `CORRELATION_ID` before inserting NOTIFICATIONS row)
3. Monitoring: alert if `OUTBOX_EVENTS` table has rows with `PUBLISHED_AT IS NULL` older than 5 minutes (indicates polling publisher failure)

---

## Review Trigger

Revisit CDC option (Debezium) when:
- Polling query takes > 100ms consistently
- Event throughput exceeds 500 events/second
- Team has capacity to operate Debezium and Kafka Connect
