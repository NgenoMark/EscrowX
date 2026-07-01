package com.example.escbackend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceTokenRequest {

    @NotBlank
    private String token;

    private String platform;
}
