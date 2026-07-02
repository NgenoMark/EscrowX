package com.example.escbackend.notification.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.notification.dto.InAppNotificationResponse;
import com.example.escbackend.notification.dto.NotificationStatusUpdateResponse;
import com.example.escbackend.notification.entity.InAppNotificationEntity;
import com.example.escbackend.notification.repository.InAppNotificationRepository;
import com.example.escbackend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InAppNotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserRepository userRepository;

    public InAppNotificationService(
        InAppNotificationRepository inAppNotificationRepository,
        UserRepository userRepository
    ) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<InAppNotificationResponse> listForUser(UUID actorUserId, int page, int size, String statusFilter) {
        userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String filter = statusFilter == null ? "" : statusFilter.trim().toUpperCase(Locale.ROOT);

        Page<InAppNotificationEntity> results;
        if (filter.isBlank() || "ACTIVE".equals(filter)) {
            results = inAppNotificationRepository.findByUserIdAndStatusNot(actorUserId, "ARCHIVED", pageable);
        } else if ("ALL".equals(filter)) {
            results = inAppNotificationRepository.findByUserIdAndStatusNot(actorUserId, "", pageable);
        } else if ("UNREAD".equals(filter) || "READ".equals(filter) || "ARCHIVED".equals(filter)) {
            results = inAppNotificationRepository.findByUserIdAndStatus(actorUserId, filter, pageable);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status filter. Use UNREAD, READ, ARCHIVED, or ACTIVE");
        }

        return results
            .map(this::toResponse);
    }

    @Transactional
    public NotificationStatusUpdateResponse markAsRead(UUID actorUserId, UUID notificationId) {
        InAppNotificationEntity entity = requireOwnedNotification(actorUserId, notificationId);

        if (!"ARCHIVED".equalsIgnoreCase(entity.getStatus())) {
            entity.setStatus("READ");
            if (entity.getReadAt() == null) {
                entity.setReadAt(OffsetDateTime.now());
            }
            inAppNotificationRepository.save(entity);
        }

        return NotificationStatusUpdateResponse.builder()
            .notificationId(entity.getId())
            .status(entity.getStatus())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    @Transactional
    public NotificationStatusUpdateResponse archive(UUID actorUserId, UUID notificationId) {
        InAppNotificationEntity entity = requireOwnedNotification(actorUserId, notificationId);

        entity.setStatus("ARCHIVED");
        if (entity.getReadAt() == null) {
            entity.setReadAt(OffsetDateTime.now());
        }
        inAppNotificationRepository.save(entity);

        return NotificationStatusUpdateResponse.builder()
            .notificationId(entity.getId())
            .status(entity.getStatus())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID actorUserId) {
        userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return inAppNotificationRepository.countByUserIdAndStatus(actorUserId, "UNREAD");
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countByStatuses(UUID actorUserId) {
        userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        long unread = inAppNotificationRepository.countByUserIdAndStatus(actorUserId, "UNREAD");
        long read = inAppNotificationRepository.countByUserIdAndStatus(actorUserId, "READ");
        long archived = inAppNotificationRepository.countByUserIdAndStatus(actorUserId, "ARCHIVED");

        return Map.of(
            "unread", unread,
            "read", read,
            "archived", archived
        );
    }

    private InAppNotificationEntity requireOwnedNotification(UUID actorUserId, UUID notificationId) {
        return inAppNotificationRepository.findByIdAndUserId(notificationId, actorUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private InAppNotificationResponse toResponse(InAppNotificationEntity entity) {
        return InAppNotificationResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .title(entity.getTitle())
            .body(entity.getBody())
            .type(entity.getType())
            .status(entity.getStatus())
            .referenceId(entity.getReferenceId())
            .referenceType(entity.getReferenceType())
            .payloadJson(entity.getPayloadJson())
            .createdAt(entity.getCreatedAt())
            .readAt(entity.getReadAt())
            .build();
    }
}
