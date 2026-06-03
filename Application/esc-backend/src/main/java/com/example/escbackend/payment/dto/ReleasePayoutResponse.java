package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ReleasePayoutResponse {
    private UUID payoutId;
    private UUID escrowId;
    private String status;
    private String conversationId;
    private String originatorConversationId;
    private String message;
}
