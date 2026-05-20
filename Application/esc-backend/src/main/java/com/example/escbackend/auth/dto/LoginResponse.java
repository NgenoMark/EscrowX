package com.example.escbackend.auth.dto;

import com.example.escbackend.user.dto.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private User user;
}
