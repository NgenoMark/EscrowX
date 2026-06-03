package com.example.escbackend.auth.controller;

import com.example.escbackend.auth.dto.ConfirmRequest;
import com.example.escbackend.auth.dto.ConfirmResponse;
import com.example.escbackend.auth.dto.LoginRequest;
import com.example.escbackend.auth.dto.LoginResponse;
import com.example.escbackend.auth.dto.LogoutRequest;
import com.example.escbackend.auth.dto.LogoutResponse;
import com.example.escbackend.auth.dto.PasswordResetConfirmRequest;
import com.example.escbackend.auth.dto.PasswordResetConfirmResponse;
import com.example.escbackend.auth.dto.PasswordResetRequestDto;
import com.example.escbackend.auth.dto.PasswordResetRequestResponse;
import com.example.escbackend.auth.dto.RefreshTokenRequest;
import com.example.escbackend.auth.dto.RefreshTokenResponse;
import com.example.escbackend.auth.dto.RegisterRequest;
import com.example.escbackend.auth.dto.RegisterResponse;
import com.example.escbackend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/confirm")
    public ConfirmResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        return authService.confirm(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/password-reset/request")
    public PasswordResetRequestResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public PasswordResetConfirmResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public LogoutResponse logout(@Valid @RequestBody LogoutRequest request) {
        return authService.logout(request);
    }
}
