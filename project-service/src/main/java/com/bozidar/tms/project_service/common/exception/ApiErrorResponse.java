package com.bozidar.tms.project_service.common.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
