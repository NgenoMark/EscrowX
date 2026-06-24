package com.example.escbackend.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayoutReconciliationResultDto {
    private int reconciledCount;
    private String message;
}
