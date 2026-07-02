package com.example.escbackend.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class InAppNotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String body;
    private String type;
    private String status;
    private UUID referenceId;
    private String referenceType;
    private Map<String, Object> payloadJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
}
