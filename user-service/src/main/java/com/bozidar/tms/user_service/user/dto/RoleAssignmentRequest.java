package com.bozidar.tms.user_service.user.dto;

import jakarta.validation.constraints.NotNull;


public record RoleAssignmentRequest(
        @NotNull String roleName
) {
}
