package com.example.escbackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminVerificationConfirmRequest {

    @NotBlank
    private String otp;

    private String reason;
}
