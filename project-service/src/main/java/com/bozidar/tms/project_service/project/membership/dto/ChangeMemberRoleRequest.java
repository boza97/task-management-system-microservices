package com.bozidar.tms.project_service.project.membership.dto;

import com.bozidar.tms.project_service.project.membership.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(
        @NotNull ProjectRole role
) {
}
