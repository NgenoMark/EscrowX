package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MpesaCallbackResponse {
    private boolean accepted;
    private String message;
}
