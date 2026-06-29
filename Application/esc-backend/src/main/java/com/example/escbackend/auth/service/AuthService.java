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
import com.example.escbackend.common.constants.BlackListStatus; // Assumed package for your new Enum
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;
    private final TokenService tokenService;
    private final UserMapperService mapper;

    public AuthService(
        UserRepository userRepository,
        ProfileRepository profileRepository,
        PasswordEncoder passwordEncoder,
        OtpService otpService,
        OtpDeliveryService otpDeliveryService,
        TokenService tokenService,
        UserMapperService mapper
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.otpDeliveryService = otpDeliveryService;
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByPhoneAndBlacklistStatusNot(request.getPhone(), BlackListStatus.NOT_BLACKLISTED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This phone number has been blacklisted from creating new accounts.");
        }
        if (userRepository.existsByEmailAndBlacklistStatusNot(request.getEmail(), BlackListStatus.NOT_BLACKLISTED)){
            throw new ApiException( HttpStatus.FORBIDDEN, "This email has been blacklisted from creating new accounts.");
        }
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
        UserRole selectedRole = request.getRole() == null ? UserRole.BUYER : request.getRole();
        if (selectedRole != UserRole.BUYER && selectedRole != UserRole.SELLER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only BUYER or SELLER can self-register");
        }
        user.setRole(selectedRole);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user = userRepository.save(user);

        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(user.getId());
        profile.setDisplayName(request.getDisplayName());
        profile.setBusinessName(request.getBusinessName());
        profileRepository.save(profile);

        String otp = otpService.generate(user.getEmail(), "REGISTER");
        otpDeliveryService.sendRegistrationOtp(user.getEmail(), otp);

        return RegisterResponse.builder()
            .userId(user.getId())
            .phone(user.getPhone())
            .status(user.getStatus())
            .role(user.getRole())
            .otpPreview(null)
            .build();
    }

    @Transactional
    public ConfirmResponse confirm(ConfirmRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        otpService.verify(request.getEmail(), "REGISTER", request.getOtp());
        if (user.getRole() == UserRole.SELLER) {
            user.setStatus(UserStatus.PENDING_ADMIN_APPROVAL);
            try {
                otpDeliveryService.sendSellerAcknowledgmentEmail(user.getEmail());
            } catch (Exception ex) {
                log.warn("Seller acknowledgment email failed for {}", user.getEmail(), ex);
            }
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        return ConfirmResponse.builder()
            .email(user.getEmail())
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

        if (user.getBlacklistStatus() != BlackListStatus.NOT_BLACKLISTED){
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been blacklisted.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your account first");
            }
            if (user.getStatus() == UserStatus.PENDING_ADMIN_APPROVAL) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Your seller account is pending admin approval");
            }
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
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String otp = otpService.generate(user.getEmail(), "PASSWORD_RESET");
        otpDeliveryService.sendPasswordResetOtp(user.getEmail(), otp);

        return PasswordResetRequestResponse.builder()
            .email(user.getEmail())
            .message("OTP sent")
            .otpPreview(null)
            .build();
    }

    @Transactional
    public PasswordResetConfirmResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        otpService.verify(request.getEmail(), "PASSWORD_RESET", request.getOtp());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return PasswordResetConfirmResponse.builder()
            .email(user.getEmail())
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
