package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InitiateStkPushResponse {
    private UUID paymentId;
    private UUID escrowId;
    private String status;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String message;
}
