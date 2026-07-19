package com.bozidar.tms.audit_service.event;

import com.bozidar.tms.audit_service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventListener {

    private final AuditLogService auditLogService;

    @KafkaListener(topics = "${app.kafka.topics.task-events}")
    public void onTaskEvent(TaskEvent event) {
        log.info("Received task event: type={}, taskId={}", event.eventType(), event.taskId());

        auditLogService.recordEvent(event);
    }
}
