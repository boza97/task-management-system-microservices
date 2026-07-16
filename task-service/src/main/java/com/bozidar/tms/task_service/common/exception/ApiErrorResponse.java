package com.bozidar.tms.task_service.common.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
