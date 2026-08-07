package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.ProjectMemberResponse;
import com.bozidar.tms.task_service.client.dto.ProjectResponse;

import java.util.Optional;
import java.util.UUID;

public interface ProjectClient {

    Optional<ProjectResponse> getProject(UUID projectId);

    Optional<ProjectMemberResponse> getMembership(UUID projectId, UUID userId);

    default boolean isMember(UUID projectId, UUID userId) {
        return getMembership(projectId, userId).isPresent();
    }

    default boolean hasRole(UUID projectId, UUID userId, String role) {
        return getMembership(projectId, userId)
                .map(m -> role.equals(m.role()))
                .orElse(false);
    }
}
