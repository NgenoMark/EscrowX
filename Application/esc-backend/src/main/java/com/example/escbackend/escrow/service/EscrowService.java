package com.example.escbackend.escrow.service;

import com.example.escbackend.common.constants.DeliveryAssignmentStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.escrow.dto.CreateEscrowTransactionRequest;
import com.example.escbackend.escrow.dto.EscrowResponse;
import com.example.escbackend.escrow.entity.DeliveryAssignmentEntity;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.DeliveryAssignmentRepository;
import com.example.escbackend.escrow.repository.EscrowRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final UserRepository userRepository;
    private final TransactionStatusHistoryService transactionStatusHistoryService;
    private final AdminAuthorizationService adminAuthorizationService;

    public EscrowService(
            EscrowRepository escrowRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            UserRepository userRepository,
            TransactionStatusHistoryService transactionStatusHistoryService,
            AdminAuthorizationService adminAuthorizationService) {
        this.escrowRepository = escrowRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.userRepository = userRepository;
        this.transactionStatusHistoryService = transactionStatusHistoryService;
        this.adminAuthorizationService = adminAuthorizationService;
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
    public EscrowResponse assignRider(UUID transactionId, UUID riderId, UUID actorUserId) {
        adminAuthorizationService.requireAdminOrSuperAdmin(actorUserId);

        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        UserEntity rider = userRepository.findById(riderId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rider not found"));

        if (rider.getRole() != UserRole.RIDER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provided user is not a RIDER");
        }

        transaction.setRider(rider);
        EscrowTransaction saved = escrowRepository.save(transaction);

        List<String> activeStatuses = DeliveryAssignmentStatus.valuesOf(
            DeliveryAssignmentStatus.ASSIGNED,
            DeliveryAssignmentStatus.ACCEPTED,
            DeliveryAssignmentStatus.PICKED_UP,
            DeliveryAssignmentStatus.IN_TRANSIT,
            DeliveryAssignmentStatus.ARRIVED_AT_BUYER
        );
        List<DeliveryAssignmentEntity> activeAssignments = deliveryAssignmentRepository
            .findByTransactionIdAndStatusIn(transactionId, activeStatuses);
        for (DeliveryAssignmentEntity activeAssignment : activeAssignments) {
            activeAssignment.setStatus(DeliveryAssignmentStatus.CANCELLED.value());
            deliveryAssignmentRepository.save(activeAssignment);
        }

        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setTransactionId(transactionId);
        assignment.setRiderUserId(riderId);
        assignment.setAssignedByUserId(actorUserId);
        assignment.setStatus(DeliveryAssignmentStatus.ASSIGNED.value());
        assignment.setDropoffAddress(saved.getDeliveryAddress());
        deliveryAssignmentRepository.save(assignment);

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
        return EscrowResponse.builder()
            .id(transaction.getId())
            .reference(transaction.getReference())
            .buyerId(transaction.getBuyer().getId())
            .sellerId(transaction.getSeller().getId())
            .riderId(transaction.getRider() != null ? transaction.getRider().getId() : null)
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
            .updatedAt(transaction.getUpdatedAt())
            .build();
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

    private DeliveryAssignmentEntity getAssignmentForAssignedRider(EscrowTransaction transaction, UUID actorUserId) {
        if (transaction.getRider() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No rider has been assigned to this transaction");
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

    private boolean isAssignmentStatus(DeliveryAssignmentEntity assignment, DeliveryAssignmentStatus status) {
        return status.value().equalsIgnoreCase(assignment.getStatus());
    }
}
