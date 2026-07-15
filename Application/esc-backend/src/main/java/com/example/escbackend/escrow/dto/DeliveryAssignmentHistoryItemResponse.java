package com.example.escbackend.escrow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryAssignmentHistoryItemResponse {

    private UUID id;
    private UUID riderUserId;
    private UUID previousRiderUserId;
    private UUID assignedByUserId;
    private String status;
    private String reassignmentReason;
    private OffsetDateTime pickedUpAt;
    private OffsetDateTime arrivedAtBuyerAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime riderMarkedDeliveredAt;
    private OffsetDateTime sellerConfirmedDeliveredAt;
    private OffsetDateTime buyerConfirmedDeliveredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
