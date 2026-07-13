package com.bozidar.tms.user_service.common.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
