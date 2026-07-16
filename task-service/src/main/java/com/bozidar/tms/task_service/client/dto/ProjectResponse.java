package com.bozidar.tms.task_service.client.dto;

import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String key,
        String name
) {
}
