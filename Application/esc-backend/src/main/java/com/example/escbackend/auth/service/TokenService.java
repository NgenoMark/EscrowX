package com.example.escbackend.auth.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.escbackend.common.exception.ApiException;

@Service
public class TokenService {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 3600;
    private final Map<String, TokenRecord> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> refreshTokens = new ConcurrentHashMap<>();
    private final Map<UUID, UserSession> activeSessionsByUser = new ConcurrentHashMap<>();

    public String createAccessToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        accessTokens.put(token, new TokenRecord(userId, OffsetDateTime.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS)));
        return token;
    }

    public String createRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        refreshTokens.put(token, new TokenRecord(userId, OffsetDateTime.now().plusDays(7)));
        return token;
    }

    public TokenPair getOrCreateLoginTokens(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        UserSession existingSession = activeSessionsByUser.get(userId);

        if (existingSession != null && existingSession.accessExpiresAt().isAfter(now)) {
            long remainingSeconds = Duration.between(now, existingSession.accessExpiresAt()).getSeconds();
            return new TokenPair(
                existingSession.accessToken(),
                existingSession.refreshToken(),
                Math.max(remainingSeconds, 0)
            );
        }

        String accessToken = createAccessToken(userId);
        String refreshToken = createRefreshToken(userId);
        OffsetDateTime accessExpiresAt = now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS);
        OffsetDateTime refreshExpiresAt = now.plusDays(7);

        activeSessionsByUser.put(
            userId,
            new UserSession(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt)
        );

        return new TokenPair(accessToken, refreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    public TokenPair refreshTokenPair(String refreshToken) {
        TokenRecord refreshRecord = refreshTokens.get(refreshToken);
        if (refreshRecord == null || refreshRecord.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        UUID userId = refreshRecord.userId();
        invalidateUserSession(userId);

        String newAccessToken = createAccessToken(userId);
        String newRefreshToken = createRefreshToken(userId);
        OffsetDateTime now = OffsetDateTime.now();
        activeSessionsByUser.put(
            userId,
            new UserSession(
                newAccessToken,
                newRefreshToken,
                now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS),
                now.plusDays(7)
            )
        );

        return new TokenPair(newAccessToken, newRefreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    public void invalidateByRefreshToken(String refreshToken) {
        TokenRecord refreshRecord = refreshTokens.get(refreshToken);
        if (refreshRecord == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        invalidateUserSession(refreshRecord.userId());
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

    private record UserSession(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessExpiresAt,
        OffsetDateTime refreshExpiresAt
    ) {
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    private void invalidateUserSession(UUID userId) {
        UserSession session = activeSessionsByUser.remove(userId);
        if (session != null) {
            accessTokens.remove(session.accessToken());
            refreshTokens.remove(session.refreshToken());
        } else {
            accessTokens.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
            refreshTokens.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
        }
    }
}
