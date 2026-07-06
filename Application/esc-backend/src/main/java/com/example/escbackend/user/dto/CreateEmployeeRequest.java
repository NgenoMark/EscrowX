package com.example.escbackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    @NotBlank
    private String displayName;

    private String businessName;
    private String address;
    private String avatarUrl;

    // Required when target role is RIDER
    private String operationArea;
    private String licenseNumber;
    private String vehicleType;
    private String vehiclePlate;
}
