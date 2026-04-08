package com.nexabank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NexaBank Transaction Service
 *
 * Owns: all money movements — deposits, withdrawals, transfers, double-entry ledger.
 *
 * Key patterns:
 * - @Transactional with compensating rollback (simplified Saga pattern)
 * - Kafka producer (transaction.completed events for notification-service)
 * - ActiveMQ JMS producer (guaranteed delivery to notification-service)
 * - Feign client with circuit breaker (calls account-service for balance checks)
 * - NDM simulation (@Scheduled batch file generation)
 *
 * See docs/learning/03-kafka-event-streaming.md
 * See docs/learning/04-activemq-mq-messaging.md
 * See docs/learning/05-ndm-batch-file-transfer.md
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
