package com.nexabank.notification.controller;

import com.nexabank.notification.model.Notification;
import com.nexabank.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Customer notification retrieval")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all notifications for a customer")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long customerId) {
        return ResponseEntity.ok(notificationService.getByCustomer(customerId));
    }

    @GetMapping("/customer/{customerId}/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long customerId) {
        return ResponseEntity.ok(Map.of("unreadCount",
                notificationService.getUnreadCount(customerId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Notification> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId));
    }
}
