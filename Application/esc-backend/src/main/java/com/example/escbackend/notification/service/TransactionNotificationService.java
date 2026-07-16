package com.example.escbackend.notification.service;

import com.example.escbackend.notification.entity.InAppNotificationEntity;
import com.example.escbackend.notification.entity.NotificationDeliveryLogEntity;
import com.example.escbackend.notification.repository.InAppNotificationRepository;
import com.example.escbackend.notification.repository.NotificationDeliveryLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionNotificationService {

    private final PushNotificationService pushNotificationService;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;

    public TransactionNotificationService(
        PushNotificationService pushNotificationService,
        InAppNotificationRepository inAppNotificationRepository,
        NotificationDeliveryLogRepository notificationDeliveryLogRepository
    ) {
        this.pushNotificationService = pushNotificationService;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.notificationDeliveryLogRepository = notificationDeliveryLogRepository;
    }

    @Transactional
    public void sendTransactionNotification(
        UUID userId,
        UUID transactionId,
        String type,
        String title,
        String body,
        String status,
        String targetRole
    ) {
        String resolvedRole = targetRole == null ? "BUYER" : targetRole.trim().toUpperCase();
        String targetScreen = "RIDER".equals(resolvedRole) ? "RIDER_ASSIGNMENT_DETAIL" : "TRANSACTION_DETAIL";

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("transactionId", transactionId.toString());
        payload.put("status", status == null ? "" : status);
        payload.put("targetRole", resolvedRole);
        payload.put("targetScreen", targetScreen);

        InAppNotificationEntity inApp = new InAppNotificationEntity();
        inApp.setUserId(userId);
        inApp.setTitle(title);
        inApp.setBody(body);
        inApp.setType(type);
        inApp.setStatus("UNREAD");
        inApp.setReferenceId(transactionId);
        inApp.setReferenceType("TRANSACTION");
        inApp.setPayloadJson(payload);
        InAppNotificationEntity savedNotification = inAppNotificationRepository.save(inApp);

        NotificationDeliveryLogEntity inAppLog = new NotificationDeliveryLogEntity();
        inAppLog.setNotificationId(savedNotification.getId());
        inAppLog.setUserId(userId);
        inAppLog.setChannel("IN_APP");
        inAppLog.setProvider("LOCAL");
        inAppLog.setStatus("SENT");
        inAppLog.setDeliveredAt(OffsetDateTime.now());
        notificationDeliveryLogRepository.save(inAppLog);

        Map<String, String> pushData = new HashMap<>();
        pushData.put("type", type);
        pushData.put("transactionId", transactionId.toString());
        pushData.put("status", status == null ? "" : status);
        pushData.put("targetRole", resolvedRole);
        pushData.put("targetScreen", targetScreen);

        try {
            PushSendResult pushResult = pushNotificationService.sendToUser(userId, title, body, pushData);

            NotificationDeliveryLogEntity pushLog = new NotificationDeliveryLogEntity();
            pushLog.setNotificationId(savedNotification.getId());
            pushLog.setUserId(userId);
            pushLog.setChannel("PUSH");
            pushLog.setProvider("FIREBASE");

            if (!pushResult.enabled() || pushResult.tokenCount() == 0) {
                pushLog.setStatus("DROPPED");
                pushLog.setErrorMessage(pushResult.message());
            } else if (pushResult.successCount() > 0) {
                pushLog.setStatus("SENT");
                pushLog.setDeliveredAt(OffsetDateTime.now());
                if (pushResult.failedCount() > 0) {
                    pushLog.setErrorMessage("Partial delivery: " + pushResult.message());
                }
            } else {
                pushLog.setStatus("FAILED");
                pushLog.setErrorMessage(pushResult.message());
            }

            notificationDeliveryLogRepository.save(pushLog);
        } catch (Exception ex) {
            NotificationDeliveryLogEntity pushLog = new NotificationDeliveryLogEntity();
            pushLog.setNotificationId(savedNotification.getId());
            pushLog.setUserId(userId);
            pushLog.setChannel("PUSH");
            pushLog.setProvider("FIREBASE");
            pushLog.setStatus("FAILED");
            pushLog.setErrorMessage(ex.getMessage());
            notificationDeliveryLogRepository.save(pushLog);
        }
    }
}
