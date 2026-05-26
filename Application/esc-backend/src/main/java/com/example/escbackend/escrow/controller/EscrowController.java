package com.example.escbackend.escrow.controller;


import com.example.escbackend.escrow.dto.CreateEscrowTransactionRequest;
import com.example.escbackend.escrow.dto.EscrowResponse;
import com.example.escbackend.escrow.service.EscrowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EscrowController {

    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService){
        this.escrowService = escrowService;
    }


    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public EscrowResponse createTransaction(@Valid @RequestBody CreateEscrowTransactionRequest request) {
        return escrowService.createTransaction(request);
    }

    @GetMapping("/transactions/{id}")
    public EscrowResponse getById(@PathVariable UUID id){
        return escrowService.getById(id);
    }

    @GetMapping("/transactions")
    public Page<EscrowResponse> listTransactions(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) OffsetDateTime dateFrom,
        @RequestParam(required = false) OffsetDateTime dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return escrowService.listTransactions(role, status, userId, dateFrom, dateTo, page, size);
    }

    @PostMapping("/transactions/{id}/accept")
    public EscrowResponse acceptTransaction(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.acceptTransaction(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/mark-in-delivery")
    public EscrowResponse markInDelivery(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.markInDelivery(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/confirm-delivery")
    public EscrowResponse confirmDelivery(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.confirmDelivery(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/confirm-receipt")
    public EscrowResponse confirmReceipt(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.confirmReceipt(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/cancel")
    public EscrowResponse cancelTransaction(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.cancelTransaction(id, actorUserId);
    }

}
