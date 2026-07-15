package com.bozidar.tms.project_service.project;

import com.bozidar.tms.project_service.project.dto.ProjectCreateRequest;
import com.bozidar.tms.project_service.project.dto.ProjectResponse;
import com.bozidar.tms.project_service.project.dto.ProjectUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ProjectResponse create(ProjectCreateRequest request);

    ProjectResponse getById(UUID projectId);

    List<ProjectResponse> getMyProjects();

    ProjectResponse update(UUID projectId, ProjectUpdateRequest request);

    void delete(UUID projectId);
}
