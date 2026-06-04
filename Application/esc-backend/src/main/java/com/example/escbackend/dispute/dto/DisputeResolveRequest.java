package com.example.escbackend.dispute.dto;

import com.example.escbackend.common.constants.DisputeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResolveRequest {

    @NotNull(message = "Provide resolution status")
    private DisputeStatus status;

    @NotBlank(message = "Provide resolution message feedback")
    private String resolution;
}
