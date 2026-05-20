package com.example.escbackend.user.dto;


import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Getter
@Builder
public class UserRoleStatusUpdateResponse {
    private UUID userId;
    private String oldValue;
    private String newValue;
    private UUID updatedBy;
    private OffsetDateTime updatedAt;
}
