package com.example.escbackend.dispute.service;

import com.example.escbackend.common.constants.DisputeStatus;
import com.example.escbackend.common.constants.EscrowStatus; // Assumed name for your transaction status enum
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.dispute.dto.*;
import com.example.escbackend.dispute.entity.DisputeEntity;
import com.example.escbackend.dispute.repository.DisputeRepository;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.EscrowRepository; // Assumed repository name
import com.example.escbackend.infrastructure.audit.AuditLogEntity;
import com.example.escbackend.infrastructure.audit.AuditLogRepository;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import com.example.escbackend.user.service.AdminAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final EscrowRepository escrowRepository;
    private final UserRepository userRepository;
    private final AdminAuthorizationService authz;
    private final AuditLogRepository auditRepo;

    public DisputeService(
            DisputeRepository disputeRepository,
            EscrowRepository escrowRepository,
            UserRepository userRepository,
            AdminAuthorizationService authz,
            AuditLogRepository auditRepo) {
        this.disputeRepository = disputeRepository;
        this.escrowRepository = escrowRepository;
        this.userRepository = userRepository;
        this.authz = authz;
        this.auditRepo = auditRepo;
    }

    @Transactional
    public DisputeCreateResponse createDispute(DisputeCreateRequest request, UUID actorUserId) {
        // 1. Check if a dispute already exists for this transaction (Guard Rail)
        if (disputeRepository.existsByTransactionId(request.getTransactionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A dispute has already been filed for this transaction.");
        }

        // 2. Fetch the target escrow transaction
        EscrowTransaction transaction = escrowRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found."));

        // Reject duplicate dispute attempts when transaction is already in disputed
        // state.
        if (EscrowStatus.DISPUTED.name().equalsIgnoreCase(transaction.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Transaction is already under dispute.");
        }

        // 3. Verify that the user raising it is actually part of the deal (Buyer or
        // Seller)
        if (!transaction.getBuyer().getId().equals(actorUserId)
                && !transaction.getSeller().getId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You are not authorized to raise a dispute against this transaction.");
        }

        // 4. Fetch user entity raising the dispute
        UserEntity raisedBy = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User entity not found."));

        // 5. Build and save the dispute
        DisputeEntity dispute = new DisputeEntity();
        dispute.setTransaction(transaction);
        dispute.setRaisedBy(raisedBy);
        dispute.setCategory(request.getCategory());
        dispute.setDescription(request.getDescription());
        dispute.setStatus(DisputeStatus.OPEN);

        dispute = disputeRepository.save(dispute);

        // 6. Push Escrow transaction status to DISPUTED
        transaction.setStatus(EscrowStatus.DISPUTED.name());
        escrowRepository.save(transaction);

        saveAudit(actorUserId, "CREATE_DISPUTE", dispute.getId(),
                "Dispute raised under category: " + request.getCategory());

        return DisputeCreateResponse.builder()
                .id(dispute.getId())
                .transactionId(transaction.getId())
                .raisedById(actorUserId)
                .category(dispute.getCategory())
                .status(dispute.getStatus())
                .createdAt(dispute.getCreatedAt())
                .build();
    }

    @Transactional
    public DisputeDetailsResponse assignAdmin(UUID disputeId, UUID adminUserId) {
        // 1. Restrict to Admin/SuperAdmin
        authz.requireAdminOrSuperAdmin(adminUserId);

        DisputeEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found."));

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot assign an admin to a closed dispute.");
        }

        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin user not found."));

        dispute.setAssignedAdmin(admin);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        disputeRepository.save(dispute);

        saveAudit(adminUserId, "ASSIGN_DISPUTE_ADMIN", disputeId, "Admin assigned to dispute case.");

        return mapToDetailsResponse(dispute);
    }

    @Transactional
    public DisputeDetailsResponse resolveDispute(UUID disputeId, UUID adminUserId, DisputeResolveRequest request) {
        authz.requireAdminOrSuperAdmin(adminUserId);

        DisputeEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found."));

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dispute is already closed.");
        }

        // Ensure the status is either RESOLVED or REJECTED
        if (request.getStatus() != DisputeStatus.RESOLVED && request.getStatus() != DisputeStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid resolution status selection.");
        }

        dispute.setStatus(request.getStatus());
        dispute.setResolution(request.getResolution());
        dispute.setResolvedAt(OffsetDateTime.now());
        disputeRepository.save(dispute);

        // Update target transaction based on resolution outcome (e.g., REFUNDED or
        // COMPLETED)
        EscrowTransaction transaction = dispute.getTransaction();
        if (request.getStatus() == DisputeStatus.RESOLVED) {
            // transaction.setStatus(EscrowStatus.REFUNDED); // Custom operational
            // preference code logic
        } else {
            // transaction.setStatus(EscrowStatus.COMPLETED);
        }
        escrowRepository.save(transaction);

        saveAudit(adminUserId, "RESOLVE_DISPUTE", disputeId, "Resolution details provided: " + request.getStatus());

        return mapToDetailsResponse(dispute);
    }

    @Transactional(readOnly = true) // <-- Add this!
    public DisputeDetailsResponse getById(UUID id, UUID actorUserId) {
        DisputeEntity dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute records not found."));

        // Security check: Only the involved buyer, seller, or an administrator can pull
        // this information
        boolean isAdmin = userRepository.findById(actorUserId)
                .map(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.SUPER_ADMIN)
                .orElse(false);

        boolean isPartToTransaction = dispute.getTransaction().getBuyer().getId().equals(actorUserId) ||
                dispute.getTransaction().getSeller().getId().equals(actorUserId);

        if (!isAdmin && !isPartToTransaction) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have authorization to view this dispute case.");
        }

        return mapToDetailsResponse(dispute);
    }

    public Page<DisputeSummaryResponse> listDisputes(String status, int page, int size, UUID adminUserId) {
        authz.requireAdminOrSuperAdmin(adminUserId);
        Pageable pageable = PageRequest.of(page, size);

        Page<DisputeEntity> disputes;
        if (status != null && !status.isBlank()) {
            DisputeStatus parsedStatus;
            try {
                parsedStatus = DisputeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid dispute status: " + status);
            }
            disputes = disputeRepository.findByStatus(parsedStatus, pageable);
        } else {
            disputes = disputeRepository.findAll(pageable);
        }

        return disputes.map(dispute -> DisputeSummaryResponse.builder()
                .id(dispute.getId())
                .transactionId(dispute.getTransaction().getId())
                .category(dispute.getCategory())
                .status(dispute.getStatus())
                .assignedAdminId(dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getId() : null)
                .createdAt(dispute.getCreatedAt())
                .build());
    }

    private DisputeDetailsResponse mapToDetailsResponse(DisputeEntity dispute) {
        return DisputeDetailsResponse.builder()
                .id(dispute.getId())
                .transactionId(dispute.getTransaction().getId())
                .transactionReference(dispute.getTransaction().getReference())
                .raisedById(dispute.getRaisedBy().getId())
                .raisedByName(dispute.getRaisedBy().getEmail()) // Defaulting safely to email, or handle profile
                                                                // fetching if required
                .category(dispute.getCategory())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .assignedAdminId(dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getId() : null)
                .resolution(dispute.getResolution())
                .resolvedAt(dispute.getResolvedAt())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }

    private void saveAudit(UUID actorId, String action, UUID entityId, String reason) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActorUserId(actorId);
        log.setAction(action);
        log.setEntityType("disputes");
        log.setEntityId(entityId);
        log.setMetadata(Map.of("reason", reason));
        auditRepo.save(log);
    }
}