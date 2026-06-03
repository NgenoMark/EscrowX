package com.example.escbackend.payment.controller;

import com.example.escbackend.payment.dto.InitiateStkPushRequest;
import com.example.escbackend.payment.dto.InitiateStkPushResponse;
import com.example.escbackend.payment.dto.PaymentResponse;
import com.example.escbackend.payment.dto.ReleasePayoutResponse;
import com.example.escbackend.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
        @Valid @RequestBody InitiateStkPushRequest request
    ) {
        return paymentService.initiateStkPush(escrowId, request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @PostMapping("/escrows/{escrowId}/release")
    public ReleasePayoutResponse releaseToSeller(@PathVariable UUID escrowId) {
        return paymentService.releaseToSeller(escrowId);
    }
}
