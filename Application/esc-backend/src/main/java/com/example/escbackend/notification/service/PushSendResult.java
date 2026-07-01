package com.example.escbackend.notification.service;

public record PushSendResult(
    boolean enabled,
    int tokenCount,
    int successCount,
    int failedCount,
    String message
) {
    public static PushSendResult disabled(String message) {

        return new PushSendResult(false, 0, 0, 0, message);
    }
}
