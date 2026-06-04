package com.example.escbackend.dispute.dto;

import com.example.escbackend.common.constants.DisputeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeCreateRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotNull(message = "Dispute category is required")
    private DisputeCategory category;

    @NotBlank(message = "Dispute description can not be empty")
    private String description;
}