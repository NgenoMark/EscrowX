package com.example.escbackend.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetRequestResponse {
    private String email;
    private String message;
    private String otpPreview;
}
