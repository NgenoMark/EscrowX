package com.example.escbackend.escrow.repository;

import com.example.escbackend.escrow.entity.DeliveryAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignmentEntity, UUID> {

    Optional<DeliveryAssignmentEntity> findTopByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    Optional<DeliveryAssignmentEntity> findTopByTransactionIdAndStatusInOrderByCreatedAtDesc(UUID transactionId, Collection<String> statuses);

    List<DeliveryAssignmentEntity> findByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    Optional<DeliveryAssignmentEntity> findTopByTransactionIdAndRiderUserIdOrderByCreatedAtDesc(UUID transactionId, UUID riderUserId);

    List<DeliveryAssignmentEntity> findByTransactionIdAndStatusIn(UUID transactionId, Collection<String> statuses);
}
