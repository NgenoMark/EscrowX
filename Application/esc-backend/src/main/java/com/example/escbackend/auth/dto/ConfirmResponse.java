package com.example.escbackend.auth.dto;

import com.example.escbackend.common.constants.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmResponse {
    private String phone;
    private UserStatus status;
    private boolean confirmed;
}
