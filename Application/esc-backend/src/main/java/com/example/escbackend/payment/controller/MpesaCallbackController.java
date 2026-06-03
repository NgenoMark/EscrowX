package com.example.escbackend.payment.controller;

import com.example.escbackend.payment.dto.MpesaCallbackResponse;
import com.example.escbackend.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/mpesa")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class MpesaCallbackController {
    private final PaymentService paymentService;

    public MpesaCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stk-callback")
    public MpesaCallbackResponse handleStkCallback(@RequestBody Map<String, Object> payload) {
        paymentService.handleStkCallback(payload);
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("STK callback processed")
            .build();
    }

    @PostMapping("/b2c-result")
    public MpesaCallbackResponse handleB2cResult(@RequestBody Map<String, Object> payload) {
        paymentService.handleB2cResult(payload);
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("B2C result processed")
            .build();
    }

    @PostMapping("/b2c-timeout")
    public MpesaCallbackResponse handleB2cTimeout(@RequestBody Map<String, Object> payload) {
        paymentService.handleB2cTimeout(payload);
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("B2C timeout processed")
            .build();
    }
}
