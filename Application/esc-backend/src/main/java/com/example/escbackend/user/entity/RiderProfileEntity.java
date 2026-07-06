package com.example.escbackend.user.entity;

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
@Table(name = "rider_profiles")
public class RiderProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "operation_area", length = 160)
    private String operationArea;

    @Column(name = "license_number", length = 80)
    private String licenseNumber;

    @Column(name = "vehicle_type", length = 40)
    private String vehicleType;

    @Column(name = "vehicle_plate", length = 40)
    private String vehiclePlate;

    @Column(name = "rider_status", nullable = false, length = 20)
    private String riderStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (riderStatus == null || riderStatus.isBlank()) {
            riderStatus = "AVAILABLE";
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
