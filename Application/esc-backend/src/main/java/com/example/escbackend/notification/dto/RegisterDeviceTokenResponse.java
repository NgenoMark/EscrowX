package com.example.escbackend.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RegisterDeviceTokenResponse {
    private UUID id;
    private UUID userId;
    private String platform;
    private boolean active;
    private OffsetDateTime lastSeenAt;
}
