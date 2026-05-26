package com.example.escbackend.auth.service;

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
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.user.dto.User;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.ProfileRepository;
import com.example.escbackend.user.repository.UserRepository;
import com.example.escbackend.user.service.UserMapperService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final UserMapperService mapper;

    public AuthService(
        UserRepository userRepository,
        ProfileRepository profileRepository,
        PasswordEncoder passwordEncoder,
        OtpService otpService,
        TokenService tokenService,
        UserMapperService mapper
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())){
            throw new ApiException(HttpStatus.CONFLICT , "Phone number already registered");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ApiException(HttpStatus.CONFLICT , "Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user = userRepository.save(user);

        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(user.getId());
        profile.setDisplayName(request.getDisplayName());
        profile.setBusinessName(request.getBusinessName());
        profileRepository.save(profile);

        String otp = otpService.generate(user.getPhone(), "REGISTER");

        return RegisterResponse.builder()
            .userId(user.getId())
            .phone(user.getPhone())
            .status(user.getStatus())
            .role(user.getRole())
            .otpPreview(otp)
            .build();
    }

    @Transactional
    public ConfirmResponse confirm(ConfirmRequest request) {
        UserEntity user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        otpService.verify(request.getPhone(), "REGISTER", request.getOtp());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return ConfirmResponse.builder()
            .phone(user.getPhone())
            .status(user.getStatus())
            .confirmed(true)
            .build();
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User account is not active");
        }

        TokenService.TokenPair tokenPair = tokenService.getOrCreateLoginTokens(user.getId());
        User responseUser = mapper.toSimple(user);

        return LoginResponse.builder()
            .accessToken(tokenPair.accessToken())
            .refreshToken(tokenPair.refreshToken())
            .tokenType("Bearer")
            .expiresIn(tokenPair.expiresInSeconds())
            .user(responseUser)
            .build();
    }

    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequestDto request) {
        UserEntity user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String otp = otpService.generate(user.getPhone(), "PASSWORD_RESET");

        return PasswordResetRequestResponse.builder()
            .phone(user.getPhone())
            .message("OTP sent")
            .otpPreview(otp)
            .build();
    }

    @Transactional
    public PasswordResetConfirmResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        UserEntity user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        otpService.verify(request.getPhone(), "PASSWORD_RESET", request.getOtp());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return PasswordResetConfirmResponse.builder()
            .phone(user.getPhone())
            .passwordUpdated(true)
            .build();
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        TokenService.TokenPair tokenPair = tokenService.refreshTokenPair(request.getRefreshToken());
        return RefreshTokenResponse.builder()
            .accessToken(tokenPair.accessToken())
            .refreshToken(tokenPair.refreshToken())
            .tokenType("Bearer")
            .expiresIn(tokenPair.expiresInSeconds())
            .build();
    }

    public LogoutResponse logout(LogoutRequest request) {
        tokenService.invalidateByRefreshToken(request.getRefreshToken());
        return LogoutResponse.builder()
            .loggedOut(true)
            .message("Logged out successfully")
            .build();
    }
}
