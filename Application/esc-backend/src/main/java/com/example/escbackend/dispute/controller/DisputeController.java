package com.example.escbackend.dispute.controller;

import com.example.escbackend.dispute.dto.*;
import com.example.escbackend.dispute.service.DisputeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * Raise a new dispute against an existing escrow transaction.
     * Accessible by: Buyers or Sellers involved in the specific deal.
     */
    @PostMapping("/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeCreateResponse createDispute(
            @Valid @RequestBody DisputeCreateRequest request,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return disputeService.createDispute(request, actorUserId);
    }

    /**
     * Get detailed information for a single dispute record.
     * Accessible by: Involved Buyer, Involved Seller, or Admins.
     */
    @GetMapping("/disputes/{id}")
    public ResponseEntity<DisputeDetailsResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return ResponseEntity.ok(disputeService.getById(id, actorUserId));
    }

    /**
     * Get dispute details by transaction id.
     * Accessible by: Involved Buyer, Involved Seller, or Admins.
     */
    @GetMapping("/disputes/transaction/{transactionId}")
    public ResponseEntity<DisputeDetailsResponse> getByTransactionId(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return ResponseEntity.ok(disputeService.getByTransactionId(transactionId, actorUserId));
    }

    @PostMapping("/disputes/{id}/evidence")
    public ResponseEntity<DisputeDetailsResponse> addEvidence(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @Valid @RequestBody DisputeEvidenceUpdateRequest request
    ) {
        return ResponseEntity.ok(disputeService.addEvidence(id, actorUserId, request));
    }

    @PostMapping("/disputes/{id}/evidence/remove")
    public ResponseEntity<DisputeDetailsResponse> removeEvidence(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @Valid @RequestBody DisputeEvidenceUpdateRequest request
    ) {
        return ResponseEntity.ok(disputeService.removeEvidence(id, actorUserId, request));
    }

    @PostMapping("/disputes/{id}/close")
    public ResponseEntity<DisputeDetailsResponse> closeDispute(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @RequestBody(required = false) DisputeCloseRequest request
    ) {
        return ResponseEntity.ok(disputeService.closeDispute(id, actorUserId, request));
    }

    /**
     * Assign an administrator to manage an open dispute case.
     * Accessible by: Admins / SuperAdmins.
     */
    @PatchMapping("/admin/disputes/{id}/assign")
    public ResponseEntity<DisputeDetailsResponse> assignAdmin(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID adminUserId
    ) {
        return ResponseEntity.ok(disputeService.assignAdmin(id, adminUserId));
    }

    /**
     * Provide a final verdict, logging the resolution details and closing out the dispute.
     * Accessible by: Admins / SuperAdmins.
     */
    @PostMapping("/admin/disputes/{id}/resolve")
    public ResponseEntity<DisputeDetailsResponse> resolveDispute(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID adminUserId,
            @Valid @RequestBody DisputeResolveRequest request
    ) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, adminUserId, request));
    }

    @PostMapping("/admin/disputes/{id}/action-required")
    public ResponseEntity<DisputeDetailsResponse> assignActionRequired(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID adminUserId,
            @Valid @RequestBody DisputeActionRequiredRequest request
    ) {
        return ResponseEntity.ok(disputeService.assignActionRequired(id, adminUserId, request));
    }

    /**
     * Retrieve a paginated list of disputes across the platform.
     * Accessible by: Admins / SuperAdmins.
     */
    @GetMapping("/admin/disputes")
    public ResponseEntity<Page<DisputeSummaryResponse>> listDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Actor-User-Id") UUID adminUserId
    ) {
        Page<DisputeSummaryResponse> disputes = disputeService.listDisputes(status, page, size, adminUserId);
        return ResponseEntity.ok(disputes);
    }
}