package com.example.escbackend.dispute.repository;

import com.example.escbackend.dispute.entity.DisputeEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidenceEntity, UUID> {
    List<DisputeEvidenceEntity> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
