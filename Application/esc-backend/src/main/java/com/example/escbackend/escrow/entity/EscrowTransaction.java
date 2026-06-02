package com.example.escbackend.escrow.entity;

import com.example.escbackend.user.entity.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "escrow_transactions")
public class EscrowTransaction {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 40)
    @NotNull
    @Column(name = "reference", nullable = false, length = 40)
    private String reference;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private UserEntity buyer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserEntity seller;

    @Size(max = 200)
    @NotNull
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @NotNull
    @Column(name = "product_description" , nullable = false , length = 2000)
    private String productDescription;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "initial_deposit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialDepositAmount;

    @Size(max = 3)
    @NotNull
    @ColumnDefault("'KES'")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Size(max = 30)
    @NotNull
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "delivery_due_at")
    private OffsetDateTime deliveryDueAt;

    @Column(name = "auto_release_at")
    private OffsetDateTime autoReleaseAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (reference == null || reference.isBlank()) {
            reference = "ESC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (currency == null || currency.isBlank()) {
            currency = "KES";
        }
        if (status == null || status.isBlank()) {
            status = "CREATED";
        }
        if (initialDepositAmount == null) {
            initialDepositAmount = amount;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
