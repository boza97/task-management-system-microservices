package com.bozidar.tms.audit_service.audit.dto;

import com.bozidar.tms.audit_service.event.TaskEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        TaskEventType actionType,
        Instant timestamp,
        String oldValue,
        String newValue,
        UUID performedById,
        String performedByName
) {
}
