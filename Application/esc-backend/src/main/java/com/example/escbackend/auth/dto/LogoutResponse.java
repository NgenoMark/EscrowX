package com.example.escbackend.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogoutResponse {
    private boolean loggedOut;
    private String message;
}
