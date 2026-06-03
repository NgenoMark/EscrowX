package com.example.escbackend.payment.entity;

import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment_intents")
public class PaymentIntentEntity {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private EscrowTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private UserEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private UserEntity seller;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_ref", length = 120)
    private String providerRef;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "checkout_request_id", length = 120)
    private String checkoutRequestId;

    @Column(name = "merchant_request_id", length = 120)
    private String merchantRequestId;

    @Column(name = "mpesa_receipt_number", length = 120)
    private String mpesaReceiptNumber;

    @Column(name = "provider_response_code", length = 30)
    private String providerResponseCode;

    @Column(name = "provider_response_description")
    private String providerResponseDescription;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
