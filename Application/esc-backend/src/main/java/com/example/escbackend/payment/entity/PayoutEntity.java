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
@Table(name = "payouts")
public class PayoutEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private EscrowTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserEntity seller;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "seller_phone_number", nullable = false, length = 20)
    private String sellerPhoneNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "conversation_id", length = 120)
    private String conversationId;

    @Column(name = "originator_conversation_id", length = 120)
    private String originatorConversationId;

    @Column(name = "result_code", length = 30)
    private String resultCode;

    @Column(name = "result_description")
    private String resultDescription;

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
