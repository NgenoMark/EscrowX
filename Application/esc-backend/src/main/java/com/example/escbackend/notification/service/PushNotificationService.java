package com.example.escbackend.notification.service;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationService {
    PushSendResult sendToUser(UUID userId, String title, String body, Map<String, String> data);
}
