package com.nexabank.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * JMS/ActiveMQ Consumer — handles events from the MQ queue.
 *
 * This is the GUARANTEED DELIVERY path for transaction events.
 * Unlike Kafka (which can replay but doesn't guarantee single delivery),
 * JMS with ActiveMQ uses:
 * - Message acknowledgement (AUTO_ACKNOWLEDGE by default)
 * - Dead letter queue for failed messages
 * - Transaction-based consumption
 *
 * In real banking:
 * - Core banking systems (like Finacle, T24) typically use MQ for settlement
 * - Kafka is used for analytics, audit, and downstream processing
 * - Having BOTH provides redundancy and matches the job description requirements
 *
 * See docs/learning/04-activemq-mq-messaging.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JmsEventHandler {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @JmsListener(
        destination = "${activemq.queue.transaction-events:nexabank.transaction.events}",
        containerFactory = "jmsListenerContainerFactory"
    )
    public void receiveTransactionEvent(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            String ref = (String) event.get("referenceNumber");
            String type = (String) event.get("type");
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

            log.info("MQ ACK: received transaction event via ActiveMQ — ref={}, type={}, amount=${}",
                    ref, type, amount);

            // MQ path creates an IN_APP notification (Kafka path handles EMAIL)
            // This demonstrates the two channels working in parallel
            Long customerId = extractCustomerId(event);
            if (customerId > 0) {
                notificationService.create(customerId, "TXN_MQ_CONFIRMED",
                        "Transaction Confirmed (MQ)",
                        String.format("MQ confirmed: %s of $%.2f (ref: %s)", type, amount, ref),
                        "nexabank.transaction.events", ref);
            }
        } catch (Exception e) {
            log.error("MQ: failed to process transaction event — message will be requeued", e);
            throw new RuntimeException("JMS processing failed", e);  // triggers redelivery
        }
    }

    private Long extractCustomerId(Map<?, ?> event) {
        Object id = event.get("customerId");
        return id != null ? Long.valueOf(id.toString()) : 0L;
    }
}
