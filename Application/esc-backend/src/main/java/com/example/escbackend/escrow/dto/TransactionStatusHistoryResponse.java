package com.example.escbackend.escrow.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class TransactionStatusHistoryResponse {
    private UUID id;
    private UUID transactionId;
    private String fromStatus;
    private String toStatus;
    private UUID changedBy;
    private String reason;
    private OffsetDateTime createdAt;
}
