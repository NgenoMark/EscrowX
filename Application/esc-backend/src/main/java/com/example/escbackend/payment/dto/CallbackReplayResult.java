package com.example.escbackend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallbackReplayResult {
    private int scanned;
    private int matched;
    private int resolved;
    private int stillUnmatched;
    private String message;
}
