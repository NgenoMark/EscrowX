package com.example.escbackend.user.entity;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.constants.BlackListStatus; // Assumed package for your new Enum
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    // Added to align with your migration strategy
    @Enumerated(EnumType.STRING)
    @Column(name = "blacklist_status", nullable = false, length = 40)
    private BlackListStatus blacklistStatus = BlackListStatus.NOT_BLACKLISTED;

    // Optional: Allows you to fetch full details directly from a user entity if it exists
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserBlacklistEntity blacklistDetails;

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
        if (blacklistStatus == null) {
            blacklistStatus = BlackListStatus.NOT_BLACKLISTED;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}