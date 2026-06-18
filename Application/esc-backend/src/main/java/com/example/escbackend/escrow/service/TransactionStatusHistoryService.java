package com.example.escbackend.escrow.service;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.dto.TransactionStatusHistoryResponse;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.entity.TransactionStatusHistoryEntity;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.escrow.repository.TransactionStatusHistoryRepository;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionStatusHistoryService {

    private final TransactionStatusHistoryRepository transactionStatusHistoryRepository;
    private final EscrowRepository escrowRepository;
    private final UserRepository userRepository;

    public TransactionStatusHistoryService(
            TransactionStatusHistoryRepository transactionStatusHistoryRepository,
            EscrowRepository escrowRepository,
            UserRepository userRepository
    ) {
        this.transactionStatusHistoryRepository = transactionStatusHistoryRepository;
        this.escrowRepository = escrowRepository;
        this.userRepository = userRepository;
    }

    public void recordStatusChange(
            EscrowTransaction transaction,
            String fromStatus,
            String toStatus,
            UUID changedBy,
            String reason
    ) {
        if (transaction == null || !Objects.equals(transaction.getStatus(), toStatus)) {
            return;
        }

        if (Objects.equals(fromStatus, toStatus)) {
            return;
        }

        TransactionStatusHistoryEntity history = new TransactionStatusHistoryEntity();
        history.setTransaction(transaction);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        transactionStatusHistoryRepository.save(history);
    }

    public List<TransactionStatusHistoryResponse> getHistoryByTransactionId(UUID transactionId, UUID actorUserId) {
        EscrowTransaction transaction = escrowRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        UserEntity actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        boolean isParticipant = transaction.getBuyer().getId().equals(actorUserId)
                || transaction.getSeller().getId().equals(actorUserId);
        boolean isAdmin = actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.SUPER_ADMIN;

        if (!isParticipant && !isAdmin) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Only the buyer, seller, admin, or super admin can view transaction status history"
            );
        }

        return transactionStatusHistoryRepository
                .findByTransaction_IdOrderByCreatedAtAsc(transactionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

        public List<TransactionStatusHistoryResponse> getAllHistory(String order, UUID actorUserId) {
                UserEntity actor = userRepository.findById(actorUserId)
                                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

                boolean isAdmin = actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.SUPER_ADMIN;
                if (!isAdmin) {
                        throw new ApiException(
                                        HttpStatus.FORBIDDEN,
                                        "Only admin or super admin can view all transaction status history"
                        );
                }

        Sort sort = "asc".equalsIgnoreCase(order)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        return transactionStatusHistoryRepository
                .findAll(sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionStatusHistoryResponse toResponse(TransactionStatusHistoryEntity history) {
        return TransactionStatusHistoryResponse.builder()
                .id(history.getId())
                .transactionId(history.getTransaction().getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
