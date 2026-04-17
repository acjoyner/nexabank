package com.nexabank.notification.service;

import com.nexabank.notification.model.Notification;
import com.nexabank.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void create_persistsNotificationWithAllFields() {
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        Notification result = notificationService.create(
                42L, "TRANSACTION", "Transfer Complete",
                "Your transfer of $300 is complete.", "nexabank.transaction.completed", "corr-123");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(42L);
        assertThat(saved.getType()).isEqualTo("TRANSACTION");
        assertThat(saved.getSubject()).isEqualTo("Transfer Complete");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCorrelationId()).isEqualTo("corr-123");
    }

    @Test
    void getByCustomer_returnsOrderedList() {
        Notification n1 = buildNotification(1L, 42L, false);
        Notification n2 = buildNotification(2L, 42L, true);
        when(notificationRepository.findByCustomerIdOrderBySentAtDesc(42L))
                .thenReturn(List.of(n1, n2));

        List<Notification> result = notificationService.getByCustomer(42L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void markRead_setsIsReadTrue() {
        Notification notification = buildNotification(10L, 42L, false);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.markRead(10L);

        assertThat(result.isRead()).isTrue();
        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    void markRead_notFound_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByCustomerIdAndIsReadFalse(42L)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(42L)).isEqualTo(3L);
    }

    private Notification buildNotification(Long id, Long customerId, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setCustomerId(customerId);
        n.setRead(read);
        n.setType("TRANSACTION");
        n.setSubject("Test");
        n.setBody("Body");
        return n;
    }
}
