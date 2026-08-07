package com.bozidar.tms.notification_service.event;

import com.bozidar.tms.notification_service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dapr")
@RequiredArgsConstructor
@Slf4j
public class DaprTaskEventSubscriber {

    private final NotificationService notificationService;

    @PostMapping("/events/task-events")
    public void onTaskEvent(@RequestBody CloudEventEnvelope envelope) {
        TaskEvent event = envelope.data();
        log.info("Received task event: type={}, taskId={}", event.eventType(), event.taskId());

        switch (event.eventType()) {
            case TASK_CREATED, ASSIGNEE_CHANGED -> notificationService.handleTaskAssigned(event);
            default -> {
            }
        }
    }
}
