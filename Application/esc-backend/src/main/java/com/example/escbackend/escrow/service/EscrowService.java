package com.example.escbackend.escrow.service;

import com.example.escbackend.audit.entity.AuditLogEntity;
import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.common.constants.DeliveryAssignmentStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.escrow.dto.CreateEscrowTransactionRequest;
import com.example.escbackend.escrow.dto.DeliveryAssignmentHistoryItemResponse;
import com.example.escbackend.escrow.dto.DeliveryAssignmentHistoryResponse;
import com.example.escbackend.escrow.dto.EscrowResponse;
import com.example.escbackend.escrow.entity.DeliveryAssignmentEntity;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.DeliveryAssignmentRepository;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.notification.service.TransactionNotificationService;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import com.example.escbackend.user.service.AdminAuthorizationService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionStatusHistoryService transactionStatusHistoryService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final TransactionNotificationService transactionNotificationService;

    public EscrowService(
            EscrowRepository escrowRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            TransactionStatusHistoryService transactionStatusHistoryService,
            AdminAuthorizationService adminAuthorizationService,
            TransactionNotificationService transactionNotificationService) {
        this.escrowRepository = escrowRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionStatusHistoryService = transactionStatusHistoryService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.transactionNotificationService = transactionNotificationService;
    }

    @Transactional
    public EscrowResponse createTransaction(CreateEscrowTransactionRequest request) {
        if (request.getBuyerId().equals(request.getSellerId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "buyerId and sellerId cannot be the same");
        }

        UserEntity buyer = userRepository.findById(request.getBuyerId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buyer not found"));
        UserEntity seller = userRepository.findById(request.getSellerId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (!StringUtils.hasText(request.getDeliveryAddress())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "deliveryAddress is required");
        }

        if (request.getInitialDepositAmount() != null && request.getInitialDepositAmount().compareTo(request.getAmount()) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Partial deposits are not supported yet. initialDepositAmount must equal amount");
        }

        EscrowTransaction transaction = new EscrowTransaction();
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setTitle(request.getTitle().trim());
        transaction.setProductDescription(request.getProductDescription().trim());
        transaction.setAmount(request.getAmount());
        transaction.setDeliveryAddress(request.getDeliveryAddress().trim());
        transaction.setInitialDepositAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency() == null ? "KES" : request.getCurrency().trim().toUpperCase(Locale.ROOT));
        transaction.setStatus("CREATED");
        transaction.setDeliveryDueAt(request.getDeliveryDueAt());
        transaction.setAutoReleaseAt(request.getDeliveryDueAt());

        EscrowTransaction saved = escrowRepository.save(transaction);
        transactionStatusHistoryService.recordStatusChange(
            saved,
            null,
            saved.getStatus(),
            request.getBuyerId(),
            "Transaction created"
        );
        return toResponse(saved);
    }

    public EscrowResponse getById(UUID id) {
        EscrowTransaction transaction = escrowRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return toResponse(transaction);
    }

    public List <EscrowResponse> getBySellerId(UUID sellerId){
        List <EscrowTransaction> transactions = escrowRepository.findBySellerId(sellerId);
        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }

    public List <EscrowResponse> getByBuyerId(UUID buyerId){
        List <EscrowTransaction> transactions = escrowRepository.findByBuyerId(buyerId);
        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EscrowResponse> getByRiderId(UUID riderId) {
        List<EscrowTransaction> transactions = escrowRepository.findByRiderId(riderId);
        return transactions.stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public DeliveryAssignmentHistoryResponse getDeliveryAssignmentHistory(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorCanViewDeliveryAssignments(transaction, actorUserId);

        List<DeliveryAssignmentEntity> assignments = deliveryAssignmentRepository
            .findByTransactionIdOrderByCreatedAtDesc(transactionId);

        UUID currentActiveAssignmentId = deliveryAssignmentRepository
            .findTopByTransactionIdAndStatusInOrderByCreatedAtDesc(
                transactionId,
                getActiveAssignmentStatuses()
            )
            .map(DeliveryAssignmentEntity::getId)
            .orElse(null);

        List<DeliveryAssignmentHistoryItemResponse> history = assignments.stream()
            .map(this::toDeliveryAssignmentHistoryItem)
            .toList();

        return DeliveryAssignmentHistoryResponse.builder()
            .transactionId(transactionId)
            .currentActiveAssignmentId(currentActiveAssignmentId)
            .assignments(history)
            .build();
    }

    @Transactional
    public EscrowResponse assignRider(UUID transactionId, UUID riderId, String reassignmentReason, UUID actorUserId) {
        adminAuthorizationService.requireAdminOrSuperAdmin(actorUserId);

        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        UUID previousRiderUserId = transaction.getRider() != null ? transaction.getRider().getId() : null;
        UserEntity rider = userRepository.findById(riderId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rider not found"));

        if (rider.getRole() != UserRole.RIDER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provided user is not a RIDER");
        }

        transaction.setRider(rider);
        EscrowTransaction saved = escrowRepository.save(transaction);

        List<String> activeStatuses = getActiveAssignmentStatuses();
        List<DeliveryAssignmentEntity> activeAssignments = deliveryAssignmentRepository
            .findByTransactionIdAndStatusIn(transactionId, activeStatuses);
        for (DeliveryAssignmentEntity activeAssignment : activeAssignments) {
            activeAssignment.setStatus(DeliveryAssignmentStatus.CANCELLED.value());
            deliveryAssignmentRepository.save(activeAssignment);
            saveDeliveryAssignmentAudit(
                actorUserId,
                "DELIVERY_ASSIGNMENT_CANCELLED",
                activeAssignment.getId(),
                Map.of(
                    "transactionId", transactionId,
                    "reason", "Reassigned to a different rider",
                    "previousRiderUserId", String.valueOf(activeAssignment.getRiderUserId())
                )
            );

            notifyDeliveryMilestone(
                saved,
                "RIDER_ASSIGNMENT_CANCELLED",
                "Delivery assignment cancelled",
                "Your assignment for " + saved.getReference() + " was cancelled due to reassignment.",
                activeAssignment.getRiderUserId(),
                actorUserId,
                DeliveryAssignmentStatus.CANCELLED.value()
            );
        }

        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setTransactionId(transactionId);
        assignment.setRiderUserId(riderId);
        assignment.setAssignedByUserId(actorUserId);
        assignment.setPreviousRiderUserId(previousRiderUserId);
        assignment.setReassignmentReason(StringUtils.hasText(reassignmentReason)
            ? reassignmentReason.trim()
            : (previousRiderUserId == null ? "Initial rider assignment" : "Rider reassigned by admin"));
        assignment.setStatus(DeliveryAssignmentStatus.ASSIGNED.value());
        assignment.setDropoffAddress(saved.getDeliveryAddress());
        DeliveryAssignmentEntity savedAssignment = deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_CREATED",
            savedAssignment.getId(),
            Map.of(
                "transactionId", transactionId,
                "newRiderUserId", String.valueOf(riderId),
                "previousRiderUserId", String.valueOf(previousRiderUserId),
                "reason", savedAssignment.getReassignmentReason()
            )
        );

        String assignmentBody = previousRiderUserId == null
            ? "You have been assigned delivery for transaction " + saved.getReference() + "."
            : "You have been reassigned delivery for transaction " + saved.getReference() + ".";
        notifyDeliveryMilestone(
            saved,
            previousRiderUserId == null ? "RIDER_ASSIGNED" : "RIDER_REASSIGNED",
            previousRiderUserId == null ? "New delivery assignment" : "Delivery reassigned to you",
            assignmentBody,
            riderId,
            actorUserId,
            DeliveryAssignmentStatus.ASSIGNED.value()
        );

        String buyerSellerTitle = previousRiderUserId == null ? "Rider assigned" : "Rider reassigned";
        String buyerSellerBody = previousRiderUserId == null
            ? "A rider has been assigned for transaction " + saved.getReference() + "."
            : "Your transaction " + saved.getReference() + " has been reassigned to a different rider.";
        notifyDeliveryMilestone(
            saved,
            previousRiderUserId == null ? "RIDER_ASSIGNED" : "RIDER_REASSIGNED",
            buyerSellerTitle,
            buyerSellerBody,
            saved.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ASSIGNED.value()
        );
        notifyDeliveryMilestone(
            saved,
            previousRiderUserId == null ? "RIDER_ASSIGNED" : "RIDER_REASSIGNED",
            buyerSellerTitle,
            buyerSellerBody,
            saved.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ASSIGNED.value()
        );

        if (!actorUserId.equals(saved.getBuyer().getId())
            && !actorUserId.equals(saved.getSeller().getId())
            && !actorUserId.equals(riderId)) {
            notifyDeliveryMilestone(
                saved,
                previousRiderUserId == null ? "RIDER_ASSIGNED" : "RIDER_REASSIGNED",
                previousRiderUserId == null ? "Rider assignment recorded" : "Rider reassignment recorded",
                "You updated rider assignment for transaction " + saved.getReference() + ".",
                actorUserId,
                actorUserId,
                DeliveryAssignmentStatus.ASSIGNED.value()
            );
        }

        return toResponse(saved);
    }


    @Transactional
    public EscrowResponse declineTransaction(UUID transactionId, UUID actorUserId){
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction, actorUserId);
        assertState(transaction,"CREATED");
        return updateTransactionStatus(transaction, "DECLINED", actorUserId, "Buyer declined transaction");

    }

    @Transactional
    public EscrowResponse approveTransaction(UUID transactionId , UUID actorUserId){
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction, actorUserId);
        assertState(transaction, "CREATED");
        return updateTransactionStatus(transaction, "PENDING_PAYMENT", actorUserId, "Buyer approved transaction");
    }

    @Transactional
    public EscrowResponse acceptTransaction(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "FUNDS_HELD");
        return updateTransactionStatus(transaction, "SELLER_ACCEPTED", actorUserId, "Seller accepted transaction");
    }

    @Transactional
    public EscrowResponse riderAcceptDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertState(transaction, "SELLER_ACCEPTED");

        DeliveryAssignmentEntity assignment = getAssignmentForAssignedRider(transaction, actorUserId);

        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.ASSIGNED)
            && !isAssignmentStatus(assignment, DeliveryAssignmentStatus.ACCEPTED)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment cannot be accepted from status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.ACCEPTED.value());
        deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_ACCEPTED",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        notifyDeliveryMilestone(
            transaction,
            "RIDER_ACCEPTED",
            "Rider accepted delivery",
            "Rider accepted delivery for transaction " + transaction.getReference() + ".",
            transaction.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ACCEPTED.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_ACCEPTED",
            "Rider accepted delivery",
            "Rider accepted delivery for transaction " + transaction.getReference() + ".",
            transaction.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ACCEPTED.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_ACCEPTED",
            "Delivery accepted",
            "You accepted delivery for transaction " + transaction.getReference() + ".",
            actorUserId,
            actorUserId,
            DeliveryAssignmentStatus.ACCEPTED.value()
        );

        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse riderMarkPickedUp(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        if (!"SELLER_ACCEPTED".equals(transaction.getStatus()) && !"IN_DELIVERY".equals(transaction.getStatus())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Rider can only mark pickup when transaction is SELLER_ACCEPTED or IN_DELIVERY"
            );
        }

        DeliveryAssignmentEntity assignment = getAssignmentForAssignedRider(transaction, actorUserId);
        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.ACCEPTED)
            && !isAssignmentStatus(assignment, DeliveryAssignmentStatus.PICKED_UP)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment cannot be picked up from status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.PICKED_UP.value());
        assignment.setPickedUpAt(OffsetDateTime.now());
        deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_PICKED_UP",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        notifyDeliveryMilestone(
            transaction,
            "RIDER_PICKED_UP",
            "Package picked up",
            "Rider picked up package for transaction " + transaction.getReference() + ".",
            transaction.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.PICKED_UP.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_PICKED_UP",
            "Package picked up",
            "Rider picked up package for transaction " + transaction.getReference() + ".",
            transaction.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.PICKED_UP.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_PICKED_UP",
            "Pickup confirmed",
            "You picked up package for transaction " + transaction.getReference() + ".",
            actorUserId,
            actorUserId,
            DeliveryAssignmentStatus.PICKED_UP.value()
        );

        if ("SELLER_ACCEPTED".equals(transaction.getStatus())) {
            return updateTransactionStatus(transaction, "IN_DELIVERY", actorUserId, "Rider picked up package");
        }

        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse riderStartTransit(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertState(transaction, "IN_DELIVERY");

        DeliveryAssignmentEntity assignment = getAssignmentForAssignedRider(transaction, actorUserId);
        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.PICKED_UP)
            && !isAssignmentStatus(assignment, DeliveryAssignmentStatus.IN_TRANSIT)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment cannot move to transit from status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.IN_TRANSIT.value());
        deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_IN_TRANSIT",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        notifyDeliveryMilestone(
            transaction,
            "RIDER_IN_TRANSIT",
            "Package in transit",
            "Rider is in transit for transaction " + transaction.getReference() + ".",
            transaction.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.IN_TRANSIT.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_IN_TRANSIT",
            "Package in transit",
            "Rider is in transit for transaction " + transaction.getReference() + ".",
            transaction.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.IN_TRANSIT.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_IN_TRANSIT",
            "Transit started",
            "You started transit for transaction " + transaction.getReference() + ".",
            actorUserId,
            actorUserId,
            DeliveryAssignmentStatus.IN_TRANSIT.value()
        );

        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse riderArrivedAtBuyer(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertState(transaction, "IN_DELIVERY");

        DeliveryAssignmentEntity assignment = getAssignmentForAssignedRider(transaction, actorUserId);
        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.IN_TRANSIT)
            && !isAssignmentStatus(assignment, DeliveryAssignmentStatus.ARRIVED_AT_BUYER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment cannot be marked arrived from status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.ARRIVED_AT_BUYER.value());
        assignment.setArrivedAtBuyerAt(OffsetDateTime.now());
        deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_ARRIVED_AT_BUYER",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        notifyDeliveryMilestone(
            transaction,
            "RIDER_ARRIVED_AT_BUYER",
            "Rider arrived",
            "Rider arrived at buyer location for transaction " + transaction.getReference() + ".",
            transaction.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ARRIVED_AT_BUYER.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_ARRIVED_AT_BUYER",
            "Rider arrived",
            "Rider arrived at buyer location for transaction " + transaction.getReference() + ".",
            transaction.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.ARRIVED_AT_BUYER.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_ARRIVED_AT_BUYER",
            "Arrival confirmed",
            "You arrived at buyer location for transaction " + transaction.getReference() + ".",
            actorUserId,
            actorUserId,
            DeliveryAssignmentStatus.ARRIVED_AT_BUYER.value()
        );

        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse riderMarkDelivered(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertState(transaction, "IN_DELIVERY");

        DeliveryAssignmentEntity assignment = getAssignmentForAssignedRider(transaction, actorUserId);
        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.ARRIVED_AT_BUYER)
            && !isAssignmentStatus(assignment, DeliveryAssignmentStatus.DELIVERED_TO_BUYER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment cannot be marked delivered from status: " + assignment.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        assignment.setStatus(DeliveryAssignmentStatus.DELIVERED_TO_BUYER.value());
        assignment.setRiderMarkedDeliveredAt(now);
        assignment.setDeliveredAt(now);
        deliveryAssignmentRepository.save(assignment);

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_ASSIGNMENT_DELIVERED",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        notifyDeliveryMilestone(
            transaction,
            "RIDER_DELIVERED",
            "Package delivered",
            "Rider marked package delivered for transaction " + transaction.getReference() + ".",
            transaction.getBuyer().getId(),
            actorUserId,
            DeliveryAssignmentStatus.DELIVERED_TO_BUYER.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_DELIVERED",
            "Package delivered",
            "Rider marked package delivered for transaction " + transaction.getReference() + ".",
            transaction.getSeller().getId(),
            actorUserId,
            DeliveryAssignmentStatus.DELIVERED_TO_BUYER.value()
        );
        notifyDeliveryMilestone(
            transaction,
            "RIDER_DELIVERED",
            "Delivery completed",
            "You marked package delivered for transaction " + transaction.getReference() + ".",
            actorUserId,
            actorUserId,
            DeliveryAssignmentStatus.DELIVERED_TO_BUYER.value()
        );

        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse markInDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "SELLER_ACCEPTED");

        if (transaction.getRider() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assign a rider before marking in delivery");
        }

        DeliveryAssignmentEntity assignment = deliveryAssignmentRepository
            .findTopByTransactionIdAndRiderUserIdOrderByCreatedAtDesc(transactionId, transaction.getRider().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No delivery assignment found for assigned rider"));

        if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.ACCEPTED)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rider must ACCEPT delivery before transaction can move to IN_DELIVERY");
        }

        saveDeliveryAssignmentAudit(
            actorUserId,
            "DELIVERY_FLOW_MARKED_IN_DELIVERY",
            assignment.getId(),
            Map.of("transactionId", transactionId)
        );

        return updateTransactionStatus(transaction, "IN_DELIVERY", actorUserId, "Seller marked in delivery");
    }

    @Transactional
    public EscrowResponse sellerConfirmDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "IN_DELIVERY");

        if (transaction.getRider() != null) {
            DeliveryAssignmentEntity assignment = deliveryAssignmentRepository
                .findTopByTransactionIdAndRiderUserIdOrderByCreatedAtDesc(transactionId, transaction.getRider().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No delivery assignment found for assigned rider"));

            if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.DELIVERED_TO_BUYER)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Rider must mark package DELIVERED before seller confirmation");
            }

            assignment.setSellerConfirmedDeliveredAt(OffsetDateTime.now());
            deliveryAssignmentRepository.save(assignment);

            saveDeliveryAssignmentAudit(
                actorUserId,
                "DELIVERY_ASSIGNMENT_SELLER_CONFIRMED",
                assignment.getId(),
                Map.of("transactionId", transactionId)
            );
        }

        return updateTransactionStatus(transaction, "SELLER_DELIVERED", actorUserId, "Seller confirmed delivery");
    }
    
    @Transactional
    public EscrowResponse buyerConfirmDelivery(UUID transactionId, UUID actorUserId){
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction , actorUserId);
        assertState(transaction , "SELLER_DELIVERED");

        if (transaction.getRider() != null) {
            deliveryAssignmentRepository
                .findTopByTransactionIdAndRiderUserIdOrderByCreatedAtDesc(transactionId, transaction.getRider().getId())
                .ifPresent(assignment -> {
                    if (!isAssignmentStatus(assignment, DeliveryAssignmentStatus.DELIVERED_TO_BUYER)) {
                        throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "Assignment must be DELIVERED_TO_BUYER before buyer confirmation"
                        );
                    }
                    assignment.setBuyerConfirmedDeliveredAt(OffsetDateTime.now());
                    deliveryAssignmentRepository.save(assignment);

                    saveDeliveryAssignmentAudit(
                        actorUserId,
                        "DELIVERY_ASSIGNMENT_BUYER_CONFIRMED",
                        assignment.getId(),
                        Map.of("transactionId", transactionId)
                    );
                });
        }

        return updateTransactionStatus(transaction, "BUYER_CONFIRMED_DELIVERED", actorUserId, "Buyer confirmed delivery");
    }

    @Transactional
    public EscrowResponse confirmReceipt(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction, actorUserId);
        assertState(transaction, "BUYER_CONFIRMED_DELIVERED");
        return updateTransactionStatus(transaction, "RELEASE_PENDING", actorUserId, "Buyer confirmed receipt");
    }

    @Transactional
    public EscrowResponse cancelTransaction(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorCanCancel(transaction, actorUserId);

        List<String> cancellable = List.of("CREATED", "PENDING_PAYMENT");
        if (!cancellable.contains(transaction.getStatus())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Transaction cannot be cancelled directly from status: " + transaction.getStatus()
                    + ". Funded transactions must use refund or dispute flow."
            );
        }

        return updateTransactionStatus(transaction, "CANCELLED", actorUserId, "Transaction cancelled");
    }

    public Page<EscrowResponse> listTransactions(
        String role,
        String status,
        UUID userId,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo,
        int page,
        int size
    ) {
        validateRoleInputs(role, userId);

        Pageable pageable = PageRequest.of(page, size);
        Specification<EscrowTransaction> specification = buildSpecification(role, status, userId, dateFrom, dateTo);
        Page<EscrowTransaction> transactions = escrowRepository.findAll(specification, pageable);
        return transactions.map(this::toResponse);
    }

    private Specification<EscrowTransaction> buildSpecification(
        String role,
        String status,
        UUID userId,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null && !role.isBlank()) {
                String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
                if ("BUYER".equals(normalizedRole)) {
                    predicates.add(cb.equal(root.get("buyer").get("id"), userId));
                } else if ("SELLER".equals(normalizedRole)) {
                    predicates.add(cb.equal(root.get("seller").get("id"), userId));
                } else if (!"ADMIN".equals(normalizedRole)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role filter. Use BUYER, SELLER, or ADMIN");
                }
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateRoleInputs(String role, UUID userId) {
        if (role == null || role.isBlank()) {
            return;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (("BUYER".equals(normalizedRole) || "SELLER".equals(normalizedRole)) && userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId is required when role is BUYER or SELLER");
        }
    }

    private EscrowResponse toResponse(EscrowTransaction transaction) {
        Optional<DeliveryAssignmentEntity> latestAssignment = deliveryAssignmentRepository
            .findTopByTransactionIdOrderByCreatedAtDesc(transaction.getId());
        Optional<DeliveryAssignmentEntity> activeAssignment = deliveryAssignmentRepository
            .findTopByTransactionIdAndStatusInOrderByCreatedAtDesc(
                transaction.getId(),
                getActiveAssignmentStatuses()
            );

        EscrowResponse.EscrowResponseBuilder builder = EscrowResponse.builder()
            .id(transaction.getId())
            .reference(transaction.getReference())
            .buyerId(transaction.getBuyer().getId())
            .sellerId(transaction.getSeller().getId())
            .riderId(transaction.getRider() != null ? transaction.getRider().getId() : null)
            .currentDeliveryAssignmentId(activeAssignment.map(DeliveryAssignmentEntity::getId).orElse(null))
            .title(transaction.getTitle())
            .productDescription(transaction.getProductDescription())
            .amount(transaction.getAmount())
                .deliveryAddress(transaction.getDeliveryAddress())
            .initialDepositAmount(transaction.getInitialDepositAmount())
            .currency(transaction.getCurrency())
            .status(transaction.getStatus())
            .deliveryDueAt(transaction.getDeliveryDueAt())
            .autoReleaseAt(transaction.getAutoReleaseAt())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt());

        latestAssignment.ifPresent(assignment -> builder
            .riderAssignmentStatus(assignment.getStatus())
            .riderPreviousRiderUserId(assignment.getPreviousRiderUserId())
            .riderReassignmentReason(assignment.getReassignmentReason())
            .riderPickedUpAt(assignment.getPickedUpAt())
            .riderArrivedAtBuyerAt(assignment.getArrivedAtBuyerAt())
            .riderDeliveredAt(assignment.getDeliveredAt())
            .riderMarkedDeliveredAt(assignment.getRiderMarkedDeliveredAt())
            .riderSellerConfirmedDeliveredAt(assignment.getSellerConfirmedDeliveredAt())
            .riderBuyerConfirmedDeliveredAt(assignment.getBuyerConfirmedDeliveredAt())
        );

        return builder.build();
    }

    private EscrowTransaction getTransactionOrThrow(UUID id) {
        return escrowRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void assertState(EscrowTransaction transaction, String expectedStatus) {
        if (!expectedStatus.equals(transaction.getStatus())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Invalid state transition. Expected " + expectedStatus + " but found " + transaction.getStatus()
            );
        }
    }

    private void assertActorIsSeller(EscrowTransaction transaction, UUID actorUserId) {
        if (!transaction.getSeller().getId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the seller can perform this action");
        }
    }

    private void assertActorIsBuyer(EscrowTransaction transaction, UUID actorUserId) {
        if (!transaction.getBuyer().getId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the buyer can perform this action");
        }
    }

    private void assertActorCanCancel(EscrowTransaction transaction, UUID actorUserId) {
        if (transaction.getBuyer().getId().equals(actorUserId) || transaction.getSeller().getId().equals(actorUserId)) {
            return;
        }

        UserEntity actor = userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (!"ADMIN".equals(actor.getRole().name()) && !"SUPER_ADMIN".equals(actor.getRole().name())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only buyer, seller, admin, or super admin can cancel");
        }
    }

    private EscrowResponse updateTransactionStatus(
        EscrowTransaction transaction,
        String newStatus,
        UUID changedBy,
        String reason
    ) {
        String fromStatus = transaction.getStatus();
        transaction.setStatus(newStatus);
        EscrowTransaction saved = escrowRepository.save(transaction);
        transactionStatusHistoryService.recordStatusChange(saved, fromStatus, newStatus, changedBy, reason);
        return toResponse(saved);
    }

    private void notifyTransactionStateChange(
        EscrowTransaction transaction,
        String fromStatus,
        String newStatus,
        UUID actorUserId,
        String reason
    ) {
        Map<UUID, String> recipients = new HashMap<>();
        recipients.put(transaction.getBuyer().getId(), "BUYER");
        recipients.put(transaction.getSeller().getId(), "SELLER");
        if (transaction.getRider() != null) {
            recipients.put(transaction.getRider().getId(), "RIDER");
        }
        if (!recipients.containsKey(actorUserId)) {
            UserEntity actor = userRepository.findById(actorUserId).orElse(null);
            recipients.put(actorUserId, actor != null ? actor.getRole().name() : "ADMIN");
        }

        for (Map.Entry<UUID, String> recipient : recipients.entrySet()) {
            UUID recipientId = recipient.getKey();
            String role = recipient.getValue();
            boolean isActor = recipientId.equals(actorUserId);

            String title = isActor
                ? "You updated transaction " + transaction.getReference()
                : "Transaction " + transaction.getReference() + " updated";
            String body = "Status changed from " + humanizeStatus(fromStatus) + " to " + humanizeStatus(newStatus) + ".";
            if (reason != null && !reason.isBlank()) {
                body = body + " " + reason;
            }

            if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                title = isActor ? "You cancelled transaction " + transaction.getReference() : "Transaction cancelled";
            }

            notifyTransactionRecipient(
                recipientId,
                transaction,
                "TRANSACTION_STATUS_" + newStatus,
                title,
                body,
                newStatus,
                role
            );
        }
    }

    private void notifyDeliveryMilestone(
        EscrowTransaction transaction,
        String type,
        String title,
        String body,
        UUID recipientUserId,
        UUID actorUserId,
        String assignmentStatus
    ) {
        UserEntity recipient = userRepository.findById(recipientUserId).orElse(null);
        if (recipient == null) {
            return;
        }
        String role = recipient.getRole().name();
        String effectiveBody = recipientUserId.equals(actorUserId)
            ? body
            : body;

        notifyTransactionRecipient(
            recipientUserId,
            transaction,
            type,
            title,
            effectiveBody,
            assignmentStatus,
            role
        );
    }

    private void notifyTransactionRecipient(
        UUID recipientId,
        EscrowTransaction transaction,
        String type,
        String title,
        String body,
        String status,
        String targetRole
    ) {
        try {
            transactionNotificationService.sendTransactionNotification(
                recipientId,
                transaction.getId(),
                type,
                title,
                body,
                status,
                targetRole
            );
        } catch (Exception ignored) {
            // Notification failures should never block escrow state transitions.
        }
    }

    private String humanizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "unknown";
        }
        return status.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private DeliveryAssignmentEntity getAssignmentForAssignedRider(EscrowTransaction transaction, UUID actorUserId) {
        if (transaction.getRider() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No rider has been assigned to this transaction");
        }

        UserEntity actor = userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (actor.getRole() != UserRole.RIDER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only a RIDER can perform rider delivery actions");
        }

        if (!transaction.getRider().getId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the assigned rider can perform this action");
        }

        DeliveryAssignmentEntity assignment = deliveryAssignmentRepository
            .findTopByTransactionIdAndRiderUserIdOrderByCreatedAtDesc(transaction.getId(), actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No active delivery assignment found for this rider"));

        if (isAssignmentStatus(assignment, DeliveryAssignmentStatus.CANCELLED)
            || isAssignmentStatus(assignment, DeliveryAssignmentStatus.FAILED)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This delivery assignment is no longer active");
        }

        return assignment;
    }

    private DeliveryAssignmentHistoryItemResponse toDeliveryAssignmentHistoryItem(DeliveryAssignmentEntity assignment) {
        return DeliveryAssignmentHistoryItemResponse.builder()
            .id(assignment.getId())
            .riderUserId(assignment.getRiderUserId())
            .previousRiderUserId(assignment.getPreviousRiderUserId())
            .assignedByUserId(assignment.getAssignedByUserId())
            .status(assignment.getStatus())
            .reassignmentReason(assignment.getReassignmentReason())
            .pickedUpAt(assignment.getPickedUpAt())
            .arrivedAtBuyerAt(assignment.getArrivedAtBuyerAt())
            .deliveredAt(assignment.getDeliveredAt())
            .riderMarkedDeliveredAt(assignment.getRiderMarkedDeliveredAt())
            .sellerConfirmedDeliveredAt(assignment.getSellerConfirmedDeliveredAt())
            .buyerConfirmedDeliveredAt(assignment.getBuyerConfirmedDeliveredAt())
            .createdAt(assignment.getCreatedAt())
            .updatedAt(assignment.getUpdatedAt())
            .build();
    }

    private void assertActorCanViewDeliveryAssignments(EscrowTransaction transaction, UUID actorUserId) {
        if (transaction.getBuyer().getId().equals(actorUserId) || transaction.getSeller().getId().equals(actorUserId)) {
            return;
        }

        if (transaction.getRider() != null && transaction.getRider().getId().equals(actorUserId)) {
            return;
        }

        UserEntity actor = userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view delivery assignment history for this transaction");
        }
    }

    private void saveDeliveryAssignmentAudit(UUID actorUserId, String action, UUID assignmentId, Map<String, Object> metadata) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setEntityType("delivery_assignments");
        log.setEntityId(assignmentId);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }

    private List<String> getActiveAssignmentStatuses() {
        return DeliveryAssignmentStatus.valuesOf(
            DeliveryAssignmentStatus.ASSIGNED,
            DeliveryAssignmentStatus.ACCEPTED,
            DeliveryAssignmentStatus.PICKED_UP,
            DeliveryAssignmentStatus.IN_TRANSIT,
            DeliveryAssignmentStatus.ARRIVED_AT_BUYER
        );
    }

    private boolean isAssignmentStatus(DeliveryAssignmentEntity assignment, DeliveryAssignmentStatus status) {
        return status.value().equalsIgnoreCase(assignment.getStatus());
    }
}
