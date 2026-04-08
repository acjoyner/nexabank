package com.nexabank.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Notification entity.
 *
 * SOURCE_TOPIC: records which Kafka topic or MQ queue triggered this notification.
 * CORRELATION_ID: the transaction referenceNumber or event ID — enables end-to-end tracing.
 * Together these two fields allow debugging across the entire event pipeline.
 */
@Entity
@Table(name = "NOTIFICATIONS", indexes = {
    @Index(name = "IDX_NOTIF_CUSTOMER", columnList = "CUSTOMER_ID"),
    @Index(name = "IDX_NOTIF_SENT_AT",  columnList = "SENT_AT")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_seq")
    @SequenceGenerator(name = "notif_seq", sequenceName = "SEQ_NOTIFICATION_ID", allocationSize = 1)
    private Long id;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String channel = "EMAIL";  // EMAIL, SMS, IN_APP

    @Column(nullable = false, length = 50)
    private String type;  // ACCOUNT_CREATED, TXN_COMPLETED, LOAN_STATUS_CHANGED

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "IS_READ", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "SENT_AT", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "READ_AT")
    private Instant readAt;

    @Column(name = "SOURCE_TOPIC", length = 100)
    private String sourceTopic;

    @Column(name = "CORRELATION_ID", length = 36)
    private String correlationId;

    @PrePersist
    protected void onCreate() {
        sentAt = Instant.now();
    }
}
