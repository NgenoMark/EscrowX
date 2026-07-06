package com.example.escbackend.escrow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "rider_user_id", nullable = false)
    private UUID riderUserId;

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "dropoff_address")
    private String dropoffAddress;

    @Column(name = "pickup_due_at")
    private OffsetDateTime pickupDueAt;

    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    @Column(name = "arrived_at_buyer_at")
    private OffsetDateTime arrivedAtBuyerAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "rider_marked_delivered_at")
    private OffsetDateTime riderMarkedDeliveredAt;

    @Column(name = "seller_confirmed_delivered_at")
    private OffsetDateTime sellerConfirmedDeliveredAt;

    @Column(name = "buyer_confirmed_delivered_at")
    private OffsetDateTime buyerConfirmedDeliveredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "ASSIGNED";
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
