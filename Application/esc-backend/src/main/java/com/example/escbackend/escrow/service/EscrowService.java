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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final UserRepository userRepository;

    public EscrowService(EscrowRepository escrowRepository, UserRepository userRepository) {
        this.escrowRepository = escrowRepository;
        this.userRepository = userRepository;
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

        if (request.getInitialDepositAmount() != null && request.getInitialDepositAmount().compareTo(request.getAmount()) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Partial deposits are not supported yet. initialDepositAmount must equal amount");
        }

        EscrowTransaction transaction = new EscrowTransaction();
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setTitle(request.getTitle().trim());
        transaction.setProductDescription(request.getProductDescription().trim());
        transaction.setAmount(request.getAmount());
        transaction.setInitialDepositAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency() == null ? "KES" : request.getCurrency().trim().toUpperCase(Locale.ROOT));
        transaction.setStatus("CREATED");
        transaction.setDeliveryDueAt(request.getDeliveryDueAt());
        transaction.setAutoReleaseAt(request.getDeliveryDueAt());

        EscrowTransaction saved = escrowRepository.save(transaction);
        return toResponse(saved);
    }

    public EscrowResponse getById(UUID id) {
        EscrowTransaction transaction = escrowRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return toResponse(transaction);
    }

    @Transactional
    public EscrowResponse acceptTransaction(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "FUNDS_HELD");
        transaction.setStatus("SELLER_ACCEPTED");
        return toResponse(escrowRepository.save(transaction));
    }

    @Transactional
    public EscrowResponse markInDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "SELLER_ACCEPTED");
        transaction.setStatus("IN_DELIVERY");
        return toResponse(escrowRepository.save(transaction));
    }

    @Transactional
    public EscrowResponse confirmDelivery(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsSeller(transaction, actorUserId);
        assertState(transaction, "IN_DELIVERY");
        transaction.setStatus("DELIVERED");
        return toResponse(escrowRepository.save(transaction));
    }

    @Transactional
    public EscrowResponse confirmReceipt(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = getTransactionOrThrow(transactionId);
        assertActorIsBuyer(transaction, actorUserId);
        assertState(transaction, "DELIVERED");
        transaction.setStatus("RELEASE_PENDING");
        return toResponse(escrowRepository.save(transaction));
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

        transaction.setStatus("CANCELLED");
        return toResponse(escrowRepository.save(transaction));
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
}
