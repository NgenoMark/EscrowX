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
public class DisputeEvidenceUpdateRequest {

    @NotBlank(message = "Evidence URL is required")
    private String evidenceUrl;
}
