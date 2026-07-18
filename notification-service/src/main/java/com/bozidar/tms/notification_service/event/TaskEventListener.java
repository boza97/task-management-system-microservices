package com.bozidar.tms.notification_service.event;

import com.bozidar.tms.notification_service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.task-events}")
    public void onTaskEvent(TaskEvent event) {
        log.info("Received task event: type={}, taskId={}", event.eventType(), event.taskId());

        switch (event.eventType()) {
            case TASK_CREATED, ASSIGNEE_CHANGED -> notificationService.handleTaskAssigned(event);
            default -> {
            }
        }
    }
}
