package com.example.escbackend.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FirebasePushNotificationService implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushNotificationService.class);
    private static final String APP_NAME = "escrowx-firebase";

    private final NotificationDeviceService notificationDeviceService;
    private final boolean enabled;
    private final String credentialsPath;
    private final String projectId;

    private volatile FirebaseMessaging messaging;

    public FirebasePushNotificationService(
        NotificationDeviceService notificationDeviceService,
        @Value("${escrowx.firebase.enabled:false}") boolean enabled,
        @Value("${escrowx.firebase.credentials.path:${escrowx.firebase.credentials-path:}}") String credentialsPath,
        @Value("${escrowx.firebase.project-id:${escrowx.firebase.projectId:}}") String projectId
    ) {
        this.notificationDeviceService = notificationDeviceService;
        this.enabled = enabled;
        this.credentialsPath = credentialsPath;
        this.projectId = projectId;
    }

    @Override
    public PushSendResult sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        Optional<FirebaseMessaging> messagingOpt = getMessaging();
        if (messagingOpt.isEmpty()) {
            String reason = enabled
                ? "Firebase messaging not initialized"
                : "Firebase push disabled";
            return PushSendResult.disabled(reason);
        }

        var tokens = notificationDeviceService.getActiveTokens(userId);
        if (tokens.isEmpty()) {
            return new PushSendResult(true, 0, 0, 0, "No active device tokens");
        }

        FirebaseMessaging firebaseMessaging = messagingOpt.get();
        int successCount = 0;
        int failedCount = 0;
        for (String token : tokens) {
            try {
                Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .build();

                firebaseMessaging.send(message);
                successCount++;
            } catch (FirebaseMessagingException ex) {
                failedCount++;
                log.warn("Failed sending FCM to token for user {}: {}", userId, ex.getMessage());
                if ("registration-token-not-registered".equals(ex.getErrorCode())) {
                    notificationDeviceService.deactivateToken(token);
                }
            } catch (Exception ex) {
                failedCount++;
                log.warn("Unexpected FCM error for user {}", userId, ex);
            }
        }

        String message = failedCount > 0
            ? "Some tokens failed"
            : "Push sent";
        return new PushSendResult(true, tokens.size(), successCount, failedCount, message);
    }

    private Optional<FirebaseMessaging> getMessaging() {
        if (!enabled) {
            return Optional.empty();
        }

        if (messaging != null) {
            return Optional.of(messaging);
        }

        synchronized (this) {
            if (messaging != null) {
                return Optional.of(messaging);
            }

            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("Firebase push enabled but escrowx.firebase.credentials.path is empty");
                return Optional.empty();
            }

            try (InputStream in = openCredentials(credentialsPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in);
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder().setCredentials(credentials);
                if (projectId != null && !projectId.isBlank()) {
                    optionsBuilder.setProjectId(projectId);
                }

                FirebaseApp app;
                try {
                    app = FirebaseApp.getInstance(APP_NAME);
                } catch (IllegalStateException ex) {
                    app = FirebaseApp.initializeApp(optionsBuilder.build(), APP_NAME);
                }

                messaging = FirebaseMessaging.getInstance(app);
                return Optional.of(messaging);
            } catch (Exception ex) {
                log.error("Failed to initialize Firebase messaging", ex);
                return Optional.empty();
            }
        }
    }

    private InputStream openCredentials(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            String classpathLocation = path.substring("classpath:".length());
            return new ClassPathResource(classpathLocation).getInputStream();
        }

        return Files.newInputStream(Path.of(path));
    }
}
