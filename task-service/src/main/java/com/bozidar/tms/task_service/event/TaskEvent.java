package com.bozidar.tms.task_service.event;

import java.time.Instant;
import java.util.UUID;

public record TaskEvent(
        UUID eventId,
        TaskEventType eventType,
        UUID taskId,
        UUID projectId,
        String taskTitle,
        String oldValue,
        String newValue,
        UUID actorId,
        String actorFullName,
        UUID assigneeId,
        Instant occurredAt
) {

    public static TaskEvent of(TaskEventType eventType,
                               UUID taskId,
                               UUID projectId,
                               String taskTitle,
                               String oldValue,
                               String newValue,
                               UUID actorId,
                               String actorFullName,
                               UUID assigneeId) {
        return new TaskEvent(
                UUID.randomUUID(),
                eventType,
                taskId,
                projectId,
                taskTitle,
                oldValue,
                newValue,
                actorId,
                actorFullName,
                assigneeId,
                Instant.now()
        );
    }
}
