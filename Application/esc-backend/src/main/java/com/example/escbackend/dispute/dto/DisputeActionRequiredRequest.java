package com.example.escbackend.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeActionRequiredRequest {

    @NotBlank(message = "Action instruction is required")
    private String action;
}
