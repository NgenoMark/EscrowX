package com.example.escbackend.dispute.dto;

import com.example.escbackend.common.constants.DisputeCategory;
import com.example.escbackend.common.constants.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeSummaryResponse {

    private UUID id;
    private UUID transactionId;
    private DisputeCategory category;
    private DisputeStatus status;
    private UUID assignedAdminId;
    private OffsetDateTime createdAt;
}
