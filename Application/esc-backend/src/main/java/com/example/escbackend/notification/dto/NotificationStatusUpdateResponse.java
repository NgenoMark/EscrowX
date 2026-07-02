package com.example.escbackend.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationStatusUpdateResponse {
    private UUID notificationId;
    private String status;
    private OffsetDateTime updatedAt;
}
