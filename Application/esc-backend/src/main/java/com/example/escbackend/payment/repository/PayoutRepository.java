package com.example.escbackend.payment.repository;

import com.example.escbackend.payment.entity.PayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<PayoutEntity, UUID> {
    Optional<PayoutEntity> findByTransactionId(UUID transactionId);

    Optional<PayoutEntity> findByConversationId(String conversationId);

    Optional<PayoutEntity> findByOriginatorConversationId(String originatorConversationId);
}
