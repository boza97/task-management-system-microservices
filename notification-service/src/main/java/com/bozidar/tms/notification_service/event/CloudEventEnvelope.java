package com.bozidar.tms.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudEventEnvelope(TaskEvent data) {
}
