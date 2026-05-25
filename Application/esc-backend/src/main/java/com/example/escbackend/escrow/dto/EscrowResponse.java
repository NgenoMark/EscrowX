package com.example.escbackend.escrow.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class EscrowResponse {

    private UUID id;
    private String reference;
    private UUID buyerId;
    private UUID sellerId;
    private String title;
    private BigDecimal amount;
    private String currency;
    private String status;
    private OffsetDateTime deliveryDueAt;
    private OffsetDateTime autoReleaseAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
