package com.example.escbackend.dispute.repository;

import com.example.escbackend.common.constants.DisputeStatus;
import com.example.escbackend.dispute.entity.DisputeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface DisputeRepository extends JpaRepository <DisputeEntity, UUID> {

    Optional<DisputeEntity> findByTransactionId(UUID transactionId);

    Page<DisputeEntity> findByStatus(DisputeStatus status, Pageable pageable);

    Page<DisputeEntity> findByAssignedAdminId(UUID assignedAdminId, Pageable pageable);

    Page<DisputeEntity> findByRaisedById(UUID raisedById , Pageable pageable);

    boolean existsByTransactionId(UUID transactionId);
}
