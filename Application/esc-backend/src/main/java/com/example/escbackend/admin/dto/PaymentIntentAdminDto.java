package com.example.escbackend.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentIntentAdminDto(
    UUID id,
    UUID transactionId,
    UUID buyerId,
    UUID sellerId,
    String provider,
    String providerRef,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String phoneNumber,
    String status,
    String checkoutRequestId,
    String merchantRequestId,
    String mpesaReceiptNumber,
    String providerResponseCode,
    String providerResponseDescription,
    OffsetDateTime paidAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
