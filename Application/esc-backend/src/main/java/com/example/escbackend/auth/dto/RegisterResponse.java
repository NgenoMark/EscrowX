package com.example.escbackend.auth.dto;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RegisterResponse {
    private UUID userId;
    private String phone;
    private UserStatus status;
    private UserRole role;
    private String otpPreview;
}
