package com.example.escbackend.notification.client;

import com.example.escbackend.notification.dto.InAppNotificationResponse;
import com.example.escbackend.notification.dto.NotificationStatusUpdateResponse;
import com.example.escbackend.notification.service.InAppNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class InAppNotificationController {

    private final InAppNotificationService inAppNotificationService;

    public InAppNotificationController(InAppNotificationService inAppNotificationService) {
        this.inAppNotificationService = inAppNotificationService;
    }

    @GetMapping
    public Page<InAppNotificationResponse> list(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status
    ) {
        return inAppNotificationService.listForUser(actorUserId, page, size, status);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-Actor-User-Id") UUID actorUserId) {
        return Map.of("unreadCount", inAppNotificationService.countUnread(actorUserId));
    }

    @GetMapping("/counts")
    public Map<String, Long> counts(@RequestHeader("X-Actor-User-Id") UUID actorUserId) {
        return inAppNotificationService.countByStatuses(actorUserId);
    }

    @PatchMapping("/{id}/read")
    public NotificationStatusUpdateResponse markAsRead(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @PathVariable UUID id
    ) {
        return inAppNotificationService.markAsRead(actorUserId, id);
    }

    @PatchMapping("/{id}/archive")
    public NotificationStatusUpdateResponse archive(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @PathVariable UUID id
    ) {
        return inAppNotificationService.archive(actorUserId, id);
    }
}
