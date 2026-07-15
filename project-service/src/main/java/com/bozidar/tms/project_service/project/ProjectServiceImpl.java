package com.bozidar.tms.project_service.project;

import com.bozidar.tms.project_service.client.UserClient;
import com.bozidar.tms.project_service.client.dto.UserResponse;
import com.bozidar.tms.project_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.project_service.project.dto.ProjectCreateRequest;
import com.bozidar.tms.project_service.project.dto.ProjectResponse;
import com.bozidar.tms.project_service.project.dto.ProjectUpdateRequest;
import com.bozidar.tms.project_service.project.membership.ProjectMembership;
import com.bozidar.tms.project_service.project.membership.ProjectMembershipRepository;
import com.bozidar.tms.project_service.project.membership.ProjectRole;
import com.bozidar.tms.project_service.security.CurrentUser;
import com.bozidar.tms.project_service.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserClient userClient;

    @Override
    public ProjectResponse create(ProjectCreateRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (projectRepository.existsByKey(request.key())) {
            throw new IllegalArgumentException("Project key already exists");
        }

        Project project = new Project(
                request.key(),
                request.name(),
                request.description(),
                currentUser.id()
        );

        projectRepository.save(project);

        ProjectMembership membership = new ProjectMembership(
                project,
                currentUser.id(),
                ProjectRole.PROJECT_MANAGER
        );

        membershipRepository.save(membership);

        return mapToResponse(project, currentUser.fullName());
    }

    @Override
    public ProjectResponse getById(UUID projectId) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!isMemberOrOwner(project, currentUser.id())) {
            throw new AccessDeniedException("Access denied");
        }

        return mapToResponse(project, resolveOwnerName(project.getOwnerId()));
    }

    @Override
    public List<ProjectResponse> getMyProjects() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        List<Project> projects = projectRepository.findAllByUser(currentUser.id());

        Set<UUID> ownerIds = projects.stream()
                                     .map(Project::getOwnerId)
                                     .collect(Collectors.toSet());

        Map<UUID, UserResponse> owners = userClient.getUsersMappedByIds(ownerIds);

        return projects.stream()
                       .map(project -> {
                           UserResponse owner = owners.get(project.getOwnerId());
                           return mapToResponse(project, owner != null ? owner.fullName() : null);
                       })
                       .toList();
    }

    @Override
    public ProjectResponse update(UUID projectId, ProjectUpdateRequest request) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!project.canBeUpdatedBy(currentUser.id())) {
            throw new AccessDeniedException("You are not allowed to update this project");
        }

        project.setName(request.name());
        project.setDescription(request.description());

        return mapToResponse(project, resolveOwnerName(project.getOwnerId()));
    }

    @Override
    public void delete(UUID projectId) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        boolean isOwner = project.isOwner(currentUser.id());
        boolean isAdmin = currentUser.hasRole("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only owner or admin can delete project");
        }

        projectRepository.delete(project);
    }

    private Project getProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private boolean isMemberOrOwner(Project project, UUID userId) {
        return project.isOwner(userId) ||
               membershipRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private String resolveOwnerName(UUID ownerId) {
        return userClient.getUser(ownerId)
                         .map(UserResponse::fullName)
                         .orElse(null);
    }

    private ProjectResponse mapToResponse(Project project, String ownerFullName) {
        return new ProjectResponse(
                project.getId(),
                project.getKey(),
                project.getName(),
                project.getDescription(),
                project.getOwnerId(),
                ownerFullName,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
