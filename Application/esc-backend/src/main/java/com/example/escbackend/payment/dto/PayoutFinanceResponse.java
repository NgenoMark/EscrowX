package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PayoutFinanceResponse {
    private UUID payoutId;
    private UUID transactionId;
    private String transactionReference;
    private UUID sellerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String conversationId;
    private String originatorConversationId;
    private String resultCode;
    private String resultDescription;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
