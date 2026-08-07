package com.bozidar.tms.audit_service.event;

import com.bozidar.tms.audit_service.audit.AuditLogService;
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

    private final AuditLogService auditLogService;

    @PostMapping("/events/task-events")
    public void onTaskEvent(@RequestBody CloudEventEnvelope envelope) {
        TaskEvent event = envelope.data();
        log.info("Received task event: type={}, taskId={}", event.eventType(), event.taskId());

        auditLogService.recordEvent(event);
    }
}
