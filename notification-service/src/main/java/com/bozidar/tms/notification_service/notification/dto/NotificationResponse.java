package com.bozidar.tms.notification_service.notification.dto;

import com.bozidar.tms.notification_service.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        UUID taskId,
        UUID projectId,
        boolean read,
        Instant createdAt
) {
}
