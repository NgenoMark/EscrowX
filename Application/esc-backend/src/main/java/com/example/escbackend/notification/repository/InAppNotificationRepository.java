package com.example.escbackend.notification.repository;

import com.example.escbackend.notification.entity.InAppNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {
	Page<InAppNotificationEntity> findByUserIdAndStatusNot(UUID userId, String status, Pageable pageable);

	Page<InAppNotificationEntity> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

	Optional<InAppNotificationEntity> findByIdAndUserId(UUID id, UUID userId);

	long countByUserIdAndStatus(UUID userId, String status);
}
