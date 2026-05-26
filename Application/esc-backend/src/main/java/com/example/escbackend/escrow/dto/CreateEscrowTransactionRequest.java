package com.example.escbackend.escrow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class CreateEscrowTransactionRequest {

    @NotNull
    private UUID buyerId;

    @NotNull
    private UUID sellerId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency;

    private OffsetDateTime deliveryDueAt;
}

