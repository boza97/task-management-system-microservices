package com.bozidar.tms.notification_service.notification;

import com.bozidar.tms.notification_service.event.TaskEvent;
import com.bozidar.tms.notification_service.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void handleTaskAssigned(TaskEvent event);

    List<NotificationResponse> getMyNotifications();

    long getUnreadCount();

    NotificationResponse markAsRead(UUID notificationId);

    void markAllAsRead();
}
