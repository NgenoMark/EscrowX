package com.example.escbackend.notification.client;

import com.example.escbackend.notification.dto.RegisterDeviceTokenRequest;
import com.example.escbackend.notification.dto.RegisterDeviceTokenResponse;
import com.example.escbackend.notification.service.NotificationDeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/devices")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificationDeviceController {

    private final NotificationDeviceService notificationDeviceService;

    public NotificationDeviceController(NotificationDeviceService notificationDeviceService) {
        this.notificationDeviceService = notificationDeviceService;
    }

    @PostMapping("/register")
    public RegisterDeviceTokenResponse register(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        return notificationDeviceService.register(actorUserId, request);
    }
}
