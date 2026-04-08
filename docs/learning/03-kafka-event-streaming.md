# Kafka Event Streaming in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What It Is
Apache Kafka is a distributed event streaming platform. Think of it as a highly durable, high-throughput message log where:
- **Producers** write events to **topics**
- **Consumers** read from topics, maintaining their own offset (position)
- Messages are retained even after consumption (replay-capable)

## Why NexaBank Uses It
Banking requires an immutable audit trail of all events. Kafka's log-based model means every transaction event is:
1. Published by transaction-service
2. Stored durably in Kafka
3. Consumed by notification-service (send alerts)
4. Available for future analytics/reporting consumers without re-publishing

## Topics in This Project

| Topic | Partitions | Producer | Consumer |
|---|---|---|---|
| `nexabank.transaction.completed` | 6 | transaction-service | notification-service |
| `nexabank.account.created` | 3 | account-service | notification-service |
| `nexabank.loan.status.changed` | 3 | loan-service | notification-service |

**Why 6 partitions for transactions?** More partitions = more parallelism. 6 partitions allows up to 6 consumer instances to process events concurrently.

## Key Code in This Project

### Producer (transaction-service)
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/service/TransactionService.java`

```java
// After persisting the transaction to DB:
kafkaTemplate.send(
    "nexabank.transaction.completed",  // topic
    txn.getReferenceNumber(),          // key (routes to same partition for same transaction)
    event                              // value (serialized to JSON)
);
```

The **key** is the referenceNumber — this ensures all events for the same transaction go to the same partition (ordering guarantee within a partition).

### Event (Java Record)
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/event/TransactionCompletedEvent.java`

Java `record` is used — immutable by design. Events should never be mutated.

### Consumer (notification-service)
**File:** `services/notification-service/src/main/java/com/nexabank/notification/service/KafkaEventHandler.java`

```java
@KafkaListener(
    topics = "nexabank.transaction.completed",
    groupId = "notification-group"
)
public void handleTransactionCompleted(ConsumerRecord<String, String> record) { ... }
```

**Consumer group "notification-group"** means: only one instance in the group processes each partition. Scale horizontally by adding instances — Kafka automatically rebalances partitions.

### Consumer Config
**File:** `services/notification-service/src/main/java/com/nexabank/notification/config/KafkaConsumerConfig.java`

`factory.setConcurrency(3)` — 3 consumer threads per instance. With 6 partitions and 2 instances × 3 threads = full parallel processing.

## Interview Talking Points

- **Why Kafka over a regular message queue?** Kafka retains messages after consumption — you can replay events for debugging, backfilling new consumers, or disaster recovery. A traditional queue deletes on consume.
- **How do you prevent duplicate processing?** Use idempotent consumers — the `referenceNumber` UUID in the event allows deduplication if the same event is processed twice.
- **What is a consumer group?** A group of consumers that collectively process a topic. Each partition is assigned to exactly one consumer in the group.
- **How would you scale notification-service?** Add more instances — Kafka auto-rebalances partitions. No code change needed.
- **At-least-once vs. exactly-once delivery?** This app uses at-least-once (simpler). Exactly-once requires Kafka transactions (`producer.initTransactions()`).

## Questions to Ask Your AI
- "What is the difference between Kafka partitions and consumer groups?"
- "How does the referenceNumber key affect Kafka partition routing?"
- "Explain what happens if notification-service is down — does it lose events?"
- "What is Kafka consumer offset and how is it committed in this app?"
- "How would I add a new consumer (e.g., an audit service) to the transaction.completed topic?"
