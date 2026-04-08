# ActiveMQ / MQ Messaging in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What It Is
ActiveMQ Artemis is a JMS (Java Message Service) message broker. It provides **guaranteed point-to-point delivery** — unlike Kafka, it uses acknowledgement-based messaging where the broker knows when a message is delivered and re-delivers on failure.

**In real banking:** IBM MQ (WebSphere MQ) is the industry standard. ActiveMQ is the open-source equivalent used here. The Spring JMS API (`JmsTemplate`, `@JmsListener`) works with both — only the connection URL changes.

## Why BOTH Kafka AND ActiveMQ?
Many banking job descriptions list both. They serve different purposes:

| | Kafka | ActiveMQ (MQ) |
|---|---|---|
| **Delivery** | At-least-once, replay-capable | Guaranteed, acknowledgement-based |
| **Pattern** | Event streaming / pub-sub | Point-to-point queue |
| **Retention** | Days/weeks | Until consumed |
| **Use case** | Audit, analytics, event sourcing | Core payment settlement, critical alerts |
| **Real banking** | Event-driven microservices | Core banking → payment gateway |

## How It Works in NexaBank

```
transaction-service
  └─> JmsTemplate.convertAndSend("nexabank.transaction.events", event)
        └─> ActiveMQ broker (port 61616)
              └─> notification-service
                    └─> @JmsListener("nexabank.transaction.events")
```

After every successful transaction, **two** messages are published:
1. Kafka `nexabank.transaction.completed` → streaming/audit path
2. ActiveMQ `nexabank.transaction.events` → guaranteed delivery path

## Key Code

### Producer (transaction-service)
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/service/MqDispatchService.java`

```java
jmsTemplate.convertAndSend(transactionEventsQueue, event);
```

`MappingJackson2MessageConverter` serializes the Java event to JSON automatically. The `_type` header allows the consumer to deserialize to the correct class.

### Config (transaction-service)
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/config/ActiveMQConfig.java`

`JmsTemplate` is the Spring abstraction for JMS — analogous to `KafkaTemplate` for Kafka.

### Consumer (notification-service)
**File:** `services/notification-service/src/main/java/com/nexabank/notification/service/JmsEventHandler.java`

```java
@JmsListener(
    destination = "nexabank.transaction.events",
    containerFactory = "jmsListenerContainerFactory"
)
public void receiveTransactionEvent(String message) { ... }
```

**Redelivery:** If `receiveTransactionEvent` throws an exception, ActiveMQ re-delivers the message (unlike Kafka where you must manually handle retries).

### Consumer Config
**File:** `services/notification-service/src/main/java/com/nexabank/notification/config/ActiveMQConsumerConfig.java`

`factory.setConcurrency("1-3")` — scales from 1 to 3 consumer threads based on load.

## NDM Connection
NDM (Sterling Connect:Direct) in banking often uses MQ as the delivery mechanism for batch files. After the `NdmFileService` generates a file, a real implementation would send an MQ message to trigger file pickup. See `docs/learning/05-ndm-batch-file-transfer.md`.

## Interview Talking Points
- **What is JMS?** Java Message Service — a standard API for messaging. `JmsTemplate` works with any JMS broker (ActiveMQ, IBM MQ, RabbitMQ with JMS plugin).
- **MQ vs Kafka for banking?** MQ for core banking settlement (guaranteed once delivery), Kafka for event streaming and analytics.
- **IBM MQ equivalent?** Change the `ActiveMQConnectionFactory` to `MQConnectionFactory` from `com.ibm.mq.jms` — the `@JmsListener` and `JmsTemplate` code stays identical.
- **What is dead letter queue?** When a message fails all redelivery attempts, it goes to the DLQ. The ActiveMQ admin console at `localhost:8161` shows DLQ messages.

## Questions to Ask Your AI
- "What is the difference between a JMS queue and a JMS topic?"
- "How would I connect this to IBM MQ instead of ActiveMQ?"
- "What is message acknowledgement and why does it matter for banking?"
- "How does the dead letter queue work in ActiveMQ?"
- "Why does throwing an exception in @JmsListener cause redelivery?"
