package com.example.escbackend.notification.repository;

import com.example.escbackend.notification.entity.NotificationDeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, UUID> {
}
