package com.example.escbackend.escrow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetDeliveryModeRequest {

    @NotBlank
    private String deliveryMode;
}
