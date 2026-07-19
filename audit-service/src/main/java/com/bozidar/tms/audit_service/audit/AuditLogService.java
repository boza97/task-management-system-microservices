package com.bozidar.tms.audit_service.audit;

import com.bozidar.tms.audit_service.audit.dto.AuditLogResponse;
import com.bozidar.tms.audit_service.event.TaskEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void recordEvent(TaskEvent event) {
        if (auditLogRepository.existsById(event.eventId())) {
            return;
        }

        AuditLog log = new AuditLog();
        log.setId(event.eventId());
        log.setTaskId(event.taskId());
        log.setProjectId(event.projectId());
        log.setActionType(event.eventType());
        log.setTimestamp(event.occurredAt());
        log.setOldValue(event.oldValue());
        log.setNewValue(event.newValue());
        log.setPerformedById(event.actorId());
        log.setPerformedByName(event.actorFullName());

        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getTaskAuditLogs(UUID taskId) {
        return auditLogRepository.findAllByTaskIdOrderByTimestampDesc(taskId).stream()
                                 .map(this::mapToResponse)
                                 .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActionType(),
                auditLog.getTimestamp(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getPerformedById(),
                auditLog.getPerformedByName()
        );
    }
}
