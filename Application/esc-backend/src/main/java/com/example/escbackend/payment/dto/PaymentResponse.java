package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private UUID escrowId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String phoneNumber;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String mpesaReceiptNumber;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
