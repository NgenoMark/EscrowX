package com.example.escbackend.payment.controller;

import com.example.escbackend.payment.dto.InitiateStkPushRequest;
import com.example.escbackend.payment.dto.InitiateStkPushResponse;
import com.example.escbackend.payment.dto.PaymentResponse;
import com.example.escbackend.payment.dto.PaymentIntentFinanceResponse;
import com.example.escbackend.payment.dto.PayoutFinanceResponse;
import com.example.escbackend.payment.dto.ReleasePayoutResponse;
import com.example.escbackend.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/escrows/{escrowId}/stk-push")
    public InitiateStkPushResponse initiateStkPush(
        @PathVariable UUID escrowId,
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody InitiateStkPushRequest request
    ) {
        return paymentService.initiateStkPush(escrowId, request, actorUserId);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @GetMapping("/intents/me")
    public List<PaymentIntentFinanceResponse> getMyPaymentIntents(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return paymentService.getMyPaymentIntents(actorUserId);
    }

    @GetMapping("/payouts/me")
    public List<PayoutFinanceResponse> getMyPayouts(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return paymentService.getMyPayouts(actorUserId);
    }

    @PostMapping("/escrows/{escrowId}/release")
    public ReleasePayoutResponse releaseToSeller(
        @PathVariable UUID escrowId,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return paymentService.releaseToSeller(escrowId, actorUserId);
    }
}
