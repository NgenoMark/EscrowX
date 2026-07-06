package com.example.escbackend.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RiderProfileResponse {

    private UUID userId;
    private String displayName;
    private String phone;
    private String operationArea;
    private String licenseNumber;
    private String vehicleType;
    private String vehiclePlate;
    private String riderStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
