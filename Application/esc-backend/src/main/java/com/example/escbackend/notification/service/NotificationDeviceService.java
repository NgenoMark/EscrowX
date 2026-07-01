package com.example.escbackend.notification.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.notification.dto.RegisterDeviceTokenRequest;
import com.example.escbackend.notification.dto.RegisterDeviceTokenResponse;
import com.example.escbackend.notification.entity.NotificationDeviceTokenEntity;
import com.example.escbackend.notification.repository.NotificationDeviceTokenRepository;
import com.example.escbackend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationDeviceService {

    private final NotificationDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public NotificationDeviceService(NotificationDeviceTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RegisterDeviceTokenResponse register(UUID actorUserId, RegisterDeviceTokenRequest request) {
        userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String normalizedToken = request.getToken() == null ? "" : request.getToken().trim();
        if (normalizedToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Device token is required");
        }

        String normalizedPlatform = request.getPlatform() == null || request.getPlatform().isBlank()
            ? "ANDROID"
            : request.getPlatform().trim().toUpperCase(Locale.ROOT);

        NotificationDeviceTokenEntity entity = tokenRepository.findByDeviceToken(normalizedToken)
            .orElseGet(NotificationDeviceTokenEntity::new);

        entity.setUserId(actorUserId);
        entity.setDeviceToken(normalizedToken);
        entity.setPlatform(normalizedPlatform);
        entity.setActive(true);
        entity.setLastSeenAt(OffsetDateTime.now());

        NotificationDeviceTokenEntity saved = tokenRepository.save(entity);

        return RegisterDeviceTokenResponse.builder()
            .id(saved.getId())
            .userId(saved.getUserId())
            .platform(saved.getPlatform())
            .active(saved.isActive())
            .lastSeenAt(saved.getLastSeenAt())
            .build();
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokens(UUID userId) {
        return tokenRepository.findByUserIdAndActiveTrue(userId).stream()
            .map(NotificationDeviceTokenEntity::getDeviceToken)
            .toList();
    }

    @Transactional
    public void deactivateToken(String token) {
        tokenRepository.findByDeviceToken(token)
            .ifPresent(entity -> {
                entity.setActive(false);
                entity.setLastSeenAt(OffsetDateTime.now());
                tokenRepository.save(entity);
            });
    }
}
