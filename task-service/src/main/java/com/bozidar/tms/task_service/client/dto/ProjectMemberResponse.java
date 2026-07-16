package com.bozidar.tms.task_service.client.dto;

import java.util.UUID;

public record ProjectMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
