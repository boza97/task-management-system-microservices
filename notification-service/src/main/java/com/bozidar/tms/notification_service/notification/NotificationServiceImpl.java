package com.bozidar.tms.notification_service.notification;

import com.bozidar.tms.notification_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.notification_service.event.TaskEvent;
import com.bozidar.tms.notification_service.notification.dto.NotificationResponse;
import com.bozidar.tms.notification_service.security.CurrentUser;
import com.bozidar.tms.notification_service.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void handleTaskAssigned(TaskEvent event) {
        UUID recipientId = event.assigneeId();

        if (recipientId == null || recipientId.equals(event.actorId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setMessage(
                event.actorFullName() + " assigned you to task \"" + event.taskTitle() + "\""
        );
        notification.setTaskId(event.taskId());
        notification.setProjectId(event.projectId());

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(currentUser.id())
                                     .stream()
                                     .map(this::mapToResponse)
                                     .toList();
    }

    @Override
    public long getUnreadCount() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.id());
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);

        return mapToResponse(notification);
    }

    @Override
    public void markAllAsRead() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        notificationRepository.markAllAsRead(currentUser.id());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getTaskId(),
                notification.getProjectId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
