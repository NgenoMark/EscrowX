package com.example.escbackend.auth.service;

import com.example.escbackend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, TokenRecord> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> refreshTokens = new ConcurrentHashMap<>();

    public String createAccessToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        accessTokens.put(token, new TokenRecord(userId, OffsetDateTime.now().plusHours(1)));
        return token;
    }

    public String createRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        refreshTokens.put(token, new TokenRecord(userId, OffsetDateTime.now().plusDays(7)));
        return token;
    }

    public UUID verifyAccessToken(String token) {
        TokenRecord record = accessTokens.get(token);
        if (record == null || record.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
        return record.userId();
    }

    public UUID verifyRefreshToken(String token) {
        TokenRecord record = refreshTokens.get(token);
        if (record == null || record.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        return record.userId();
    }

    private record TokenRecord(UUID userId, OffsetDateTime expiresAt) {
    }
}
