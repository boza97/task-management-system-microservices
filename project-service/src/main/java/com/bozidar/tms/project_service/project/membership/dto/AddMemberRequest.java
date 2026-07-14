package com.bozidar.tms.project_service.project.membership.dto;

import com.bozidar.tms.project_service.project.membership.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull ProjectRole role
) {
}
