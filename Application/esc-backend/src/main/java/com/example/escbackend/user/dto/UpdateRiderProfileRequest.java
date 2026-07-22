package com.example.escbackend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRiderProfileRequest {

    private String operationArea;
    private String licenseNumber;
    private String vehicleType;
    private String vehiclePlate;
    private String riderStatus;
}
