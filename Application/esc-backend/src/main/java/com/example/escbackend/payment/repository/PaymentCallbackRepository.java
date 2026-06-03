package com.example.escbackend.payment.repository;

import com.example.escbackend.payment.entity.PaymentCallbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentCallbackRepository extends JpaRepository<PaymentCallbackEntity, UUID> {
}
