package com.example.escbackend.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PayoutAdminDto(
    UUID id,
    UUID transactionId,
    UUID sellerId,
    String provider,
    BigDecimal amount,
    String currency,
    String sellerPhoneNumber,
    String status,
    String conversationId,
    String originatorConversationId,
    String resultCode,
    String resultDescription,
    OffsetDateTime paidAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
