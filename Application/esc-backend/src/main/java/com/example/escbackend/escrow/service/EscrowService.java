package com.example.escbackend.escrow.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.dto.CreateEscrowTransactionRequest;
import com.example.escbackend.escrow.dto.EscrowResponse;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final TransactionStatusHistoryService transactionStatusHistoryService;

    public EscrowService(
            EscrowRepository escrowRepository,
            UserRepository userRepository,
            TransactionStatusHistoryService transactionStatusHistoryService) {
        this.escrowRepository = escrowRepository;
        this.userRepository = userRepository;
        this.transactionStatusHistoryService = transactionStatusHistoryService;
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

        if (transactions.isEmpty()){
                throw new ApiException(HttpStatus.NOT_FOUND, "No transactions found for this seller");
        }

        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }

    public List <EscrowResponse> getByBuyerId(UUID buyerId){
        List <EscrowTransaction> transactions = escrowRepository.findByBuyerId(buyerId);

        if (transactions.isEmpty()){
            throw new ApiException(HttpStatus.NOT_FOUND , "No transactions found for this buyer");
        }

        return transactions.stream()
                .map(this::toResponse)
                .toList();
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
    public EscrowResponse markInDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "SELLER_ACCEPTED");
        return updateTransactionStatus(transaction, "IN_DELIVERY", actorUserId, "Seller marked in delivery");
    }

    @Transactional
    public EscrowResponse sellerConfirmDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "IN_DELIVERY");
        return updateTransactionStatus(transaction, "SELLER_DELIVERED", actorUserId, "Seller confirmed delivery");
    }
    
    @Transactional
    public EscrowResponse buyerConfirmDelivery(UUID transactionId, UUID actorUserId){
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction , actorUserId);
        assertState(transaction , "SELLER_DELIVERED");
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
}
