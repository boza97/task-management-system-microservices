package com.bozidar.tms.audit_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudEventEnvelope(TaskEvent data) {
}
