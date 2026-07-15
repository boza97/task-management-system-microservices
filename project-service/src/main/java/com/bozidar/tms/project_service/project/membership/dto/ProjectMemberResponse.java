package com.bozidar.tms.project_service.project.membership.dto;

import com.bozidar.tms.project_service.project.membership.ProjectRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        ProjectRole role,
        Instant joinedAt
) {
}
