package com.example.escbackend.notification.repository;

import com.example.escbackend.notification.entity.NotificationDeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceTokenEntity, UUID> {
    Optional<NotificationDeviceTokenEntity> findByDeviceToken(String deviceToken);

    List<NotificationDeviceTokenEntity> findByUserIdAndActiveTrue(UUID userId);
}
