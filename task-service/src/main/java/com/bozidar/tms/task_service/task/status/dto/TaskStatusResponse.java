package com.bozidar.tms.task_service.task.status.dto;

public record TaskStatusResponse(
        String code,
        String name,
        int displayOrder
) {
}
