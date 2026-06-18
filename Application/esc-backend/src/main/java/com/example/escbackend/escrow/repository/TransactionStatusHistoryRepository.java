package com.example.escbackend.escrow.repository;

import com.example.escbackend.escrow.entity.TransactionStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistoryEntity, UUID> {
	List<TransactionStatusHistoryEntity> findByTransaction_IdOrderByCreatedAtAsc(UUID transactionId);
}
