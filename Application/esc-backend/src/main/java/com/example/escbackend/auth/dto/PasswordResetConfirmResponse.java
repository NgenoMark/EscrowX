package com.example.escbackend.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetConfirmResponse {
    private String email;
    private boolean passwordUpdated;
}
