package com.example.escbackend.dispute.entity;

import com.example.escbackend.common.constants.DisputeCategory;
import com.example.escbackend.common.constants.DisputeStatus;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.user.entity.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "disputes")
public class DisputeEntity {

    @Id
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn( name = "transaction_id", nullable = false, unique = true)
    private EscrowTransaction transaction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raised_by", nullable = false)
    private UserEntity raisedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category" , nullable = false, length = 40)
    private DisputeCategory category;

    @NotNull
    @Column(name = "description" , nullable =  false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false , length = 40)
    private DisputeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private UserEntity assignedAdmin;

    @Column(name = "resolution" , columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        OffsetDateTime now = OffsetDateTime.now();
        if(id == null){
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = OffsetDateTime.now();
    }

}