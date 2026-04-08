package com.nexabank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NexaBank Notification Service
 *
 * Purely event-driven — no inbound HTTP from users, only event consumption:
 * - Kafka consumer: nexabank.transaction.completed, nexabank.account.created, nexabank.loan.status.changed
 * - ActiveMQ/JMS consumer: nexabank.transaction.events (MQ guaranteed delivery path)
 *
 * Outbound HTTP: GET /api/notifications/{customerId}, PATCH /{id}/read
 *
 * The dual consumer pattern (Kafka + MQ) demonstrates both technologies:
 * - Kafka: event streaming, replay-capable, analytics
 * - ActiveMQ: guaranteed delivery, traditional banking MQ integration
 *
 * See docs/learning/03-kafka-event-streaming.md
 * See docs/learning/04-activemq-mq-messaging.md
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
