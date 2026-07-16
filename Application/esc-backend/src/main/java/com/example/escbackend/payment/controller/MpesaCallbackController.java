package com.example.escbackend.payment.controller;

import com.example.escbackend.payment.dto.MpesaCallbackResponse;
import com.example.escbackend.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/mpesa")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class MpesaCallbackController {
    private static final Logger log = LoggerFactory.getLogger(MpesaCallbackController.class);

    private final PaymentService paymentService;

    public MpesaCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stk-callback")
    public MpesaCallbackResponse handleStkCallback(@RequestBody Map<String, Object> payload) {
        try {
            paymentService.handleStkCallback(payload);
        } catch (Exception ex) {
            log.error("STK callback processing failed but returning accepted response", ex);
            return MpesaCallbackResponse.builder()
                .accepted(true)
                .message("STK callback accepted for asynchronous reconciliation")
                .build();
        }
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("STK callback processed")
            .build();
    }

    @PostMapping("/b2c/result")
    public MpesaCallbackResponse handleB2cResult(@RequestBody Map<String, Object> payload) {
        try {
            paymentService.handleB2cResult(payload);
        } catch (Exception ex) {
            log.error("B2C result callback processing failed but returning accepted response", ex);
            return MpesaCallbackResponse.builder()
                .accepted(true)
                .message("B2C result callback accepted for asynchronous reconciliation")
                .build();
        }
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("B2C result processed")
            .build();
    }

    @PostMapping("/b2c/queue")
    public MpesaCallbackResponse handleB2cTimeout(@RequestBody Map<String, Object> payload) {
        try {
            paymentService.handleB2cTimeout(payload);
        } catch (Exception ex) {
            log.error("B2C timeout callback processing failed but returning accepted response", ex);
            return MpesaCallbackResponse.builder()
                .accepted(true)
                .message("B2C timeout callback accepted for asynchronous reconciliation")
                .build();
        }
        return MpesaCallbackResponse.builder()
            .accepted(true)
            .message("B2C timeout processed")
            .build();
    }
}
