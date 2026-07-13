package com.example.escbackend.escrow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignRiderRequest {

    @NotNull(message = "riderId is required")
    private UUID riderId;

    private String reassignmentReason;
}
