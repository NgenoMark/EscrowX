package com.example.escbackend.payment.entity;

import com.example.escbackend.escrow.entity.EscrowTransaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "escrow_ledger_entries")
public class EscrowLedgerEntryEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private EscrowTransaction transaction;

    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }
}
