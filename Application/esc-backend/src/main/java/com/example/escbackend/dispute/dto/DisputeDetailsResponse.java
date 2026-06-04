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
public class DisputeDetailsResponse {

    private UUID id;
    private UUID transactionId;
    private String transactionReference;
    private UUID raisedById;
    private String raisedByName;
    private DisputeCategory category;
    private String description;
    private DisputeStatus status;
    private UUID assignedAdminId;
    private String resolution;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
