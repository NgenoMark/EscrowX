package com.example.escbackend.escrow.controller;


import com.example.escbackend.escrow.dto.AssignRiderRequest;
import com.example.escbackend.escrow.dto.CreateEscrowTransactionRequest;
import com.example.escbackend.escrow.dto.EscrowResponse;
import com.example.escbackend.escrow.service.EscrowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class EscrowController {

    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService){
        this.escrowService = escrowService;
    }


    @PostMapping("/transactions/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EscrowResponse createTransaction(@Valid @RequestBody CreateEscrowTransactionRequest request) {
        return escrowService.createTransaction(request);
    }

    @GetMapping("/transactions/{id}")
    public EscrowResponse getById(@PathVariable UUID id){
        return escrowService.getById(id);
    }

    @GetMapping("/transactions/seller/{sellerId}")
    public List<EscrowResponse> getBySellerId(@PathVariable UUID sellerId){
        return escrowService.getBySellerId(sellerId);}

    @GetMapping("/transactions/buyer/{buyerId}")
    public List <EscrowResponse> getByBuyerId(@PathVariable UUID buyerId){
        return escrowService.getByBuyerId(buyerId);
    }

    @GetMapping("/transactions/rider/{riderId}")
    public List<EscrowResponse> getByRiderId(@PathVariable UUID riderId) {
        return escrowService.getByRiderId(riderId);
    }

    @GetMapping("/transactions/search")
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


    
    @PostMapping("/transactions/{id}/decline-transaction")
    public EscrowResponse declineTransactionPlural(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.declineTransaction(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/approve-transaction")
    public EscrowResponse approveTransaction(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    )
    {
        return escrowService.approveTransaction(id, actorUserId);
    }

    @PostMapping("/transactions/{id}/accept-transaction")
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

    @PostMapping("/transactions/{id}/assign-rider")
    public EscrowResponse assignRider(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @Valid @RequestBody AssignRiderRequest request
    ) {
        return escrowService.assignRider(id, request.getRiderId(), actorUserId);
    }

    @PostMapping("/transactions/{id}/seller-confirm-delivery")
    public EscrowResponse sellerConfirmDelivery(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return escrowService.sellerConfirmDelivery(id, actorUserId);
    }


    @PostMapping("/transactions/{id}/buyer-confirm-delivery")
    public EscrowResponse buyerConfirmDelivery(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ){
        return escrowService.buyerConfirmDelivery(id, actorUserId);
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
