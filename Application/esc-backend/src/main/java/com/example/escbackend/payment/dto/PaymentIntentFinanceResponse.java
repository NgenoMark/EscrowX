package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentIntentFinanceResponse {
    private UUID paymentId;
    private UUID transactionId;
    private String transactionReference;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String phoneNumber;
    private String mpesaReceiptNumber;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
