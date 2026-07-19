package com.bozidar.tms.audit_service.event;

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
}
