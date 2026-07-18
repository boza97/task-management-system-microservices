package com.bozidar.tms.notification_service.common.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
