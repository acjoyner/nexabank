package com.nexabank.transaction.service;

import com.nexabank.transaction.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 * MQ Dispatch Service — sends transaction events to ActiveMQ.
 *
 * WHY both Kafka AND ActiveMQ?
 * - Kafka: high-throughput event streaming, replay-capable, for analytics/audit
 * - ActiveMQ (MQ): guaranteed point-to-point delivery, acknowledgement-based,
 *   traditional banking systems often require MQ for core payment flows
 *
 * This mirrors IBM MQ usage in real banking environments (NDM/MQ integration).
 * ActiveMQ Artemis is used here as an open-source equivalent to IBM MQ.
 *
 * See docs/learning/04-activemq-mq-messaging.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MqDispatchService {

    private final JmsTemplate jmsTemplate;

    @Value("${activemq.queue.transaction-events:nexabank.transaction.events}")
    private String transactionEventsQueue;

    public void dispatch(TransactionCompletedEvent event) {
        try {
            jmsTemplate.convertAndSend(transactionEventsQueue, event);
            log.info("MQ: dispatched transaction {} to queue {}",
                    event.referenceNumber(), transactionEventsQueue);
        } catch (Exception e) {
            // MQ dispatch failure should NOT roll back the DB transaction
            // Log and continue — Kafka already provides the audit trail
            log.error("MQ dispatch failed for transaction {}: {}",
                    event.referenceNumber(), e.getMessage());
        }
    }
}
