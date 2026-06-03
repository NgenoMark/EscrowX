package com.example.escbackend.payment.repository;

import com.example.escbackend.payment.entity.PaymentIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, UUID> {
    Optional<PaymentIntentEntity> findByTransactionId(UUID transactionId);

    Optional<PaymentIntentEntity> findByCheckoutRequestId(String checkoutRequestId);
}
