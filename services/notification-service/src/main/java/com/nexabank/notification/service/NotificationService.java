package com.nexabank.notification.service;

import com.nexabank.notification.model.Notification;
import com.nexabank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(Long customerId, String type, String subject,
                               String body, String sourceTopic, String correlationId) {
        Notification notification = Notification.builder()
                .customerId(customerId)
                .type(type)
                .subject(subject)
                .body(body)
                .channel("EMAIL")
                .sourceTopic(sourceTopic)
                .correlationId(correlationId)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getByCustomer(Long customerId) {
        return notificationRepository.findByCustomerIdOrderBySentAtDesc(customerId);
    }

    @Transactional
    public Notification markRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setIsRead(true);
        n.setReadAt(Instant.now());
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }
}
