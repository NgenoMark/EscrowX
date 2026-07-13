package com.example.escbackend.escrow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DeliveryAssignmentHistoryResponse {

    private UUID transactionId;
    private UUID currentActiveAssignmentId;
    private List<DeliveryAssignmentHistoryItemResponse> assignments;
}
