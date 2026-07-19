package com.bozidar.tms.audit_service.common.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
