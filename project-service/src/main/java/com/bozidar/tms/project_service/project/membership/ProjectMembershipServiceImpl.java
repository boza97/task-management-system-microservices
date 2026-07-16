package com.bozidar.tms.project_service.project.membership;

import com.bozidar.tms.project_service.client.UserClient;
import com.bozidar.tms.project_service.client.dto.UserResponse;
import com.bozidar.tms.project_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.project_service.project.Project;
import com.bozidar.tms.project_service.project.ProjectRepository;
import com.bozidar.tms.project_service.project.membership.dto.AddMemberRequest;
import com.bozidar.tms.project_service.project.membership.dto.ChangeMemberRoleRequest;
import com.bozidar.tms.project_service.project.membership.dto.ProjectMemberResponse;
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
public class ProjectMembershipServiceImpl implements ProjectMembershipService {

    private final ProjectMembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserClient userClient;

    @Override
    public ProjectMemberResponse addMember(UUID projectId, AddMemberRequest request) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!project.canBeUpdatedBy(currentUser.id())) {
            throw new AccessDeniedException("Not allowed to add members");
        }

        UserResponse member = userClient.getUser(request.userId())
                                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (membershipRepository.existsByProjectIdAndUserId(projectId, member.id())) {
            throw new IllegalArgumentException("User is already a member");
        }

        ProjectMembership membership = new ProjectMembership(project, member.id(), request.role());

        membership = membershipRepository.save(membership);

        return mapToResponse(membership, member);
    }

    @Override
    public List<ProjectMemberResponse> listMembers(UUID projectId) {
        getProjectOrThrow(projectId);

        List<ProjectMembership> memberships = membershipRepository.findAllByProjectId(projectId);

        Set<UUID> userIds = memberships.stream()
                                       .map(ProjectMembership::getUserId)
                                       .collect(Collectors.toSet());

        Map<UUID, UserResponse> users = userClient.getUsersMappedByIds(userIds);

        return memberships.stream()
                          .map(membership -> mapToResponse(membership, users.get(membership.getUserId())))
                          .toList();
    }

    @Override
    public ProjectMemberResponse getMember(UUID projectId, UUID userId) {
        getProjectOrThrow(projectId);

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Membership not found"));

        UserResponse user = userClient.getUser(userId).orElse(null);

        return mapToResponse(membership, user);
    }

    @Override
    public ProjectMemberResponse changeRole(UUID projectId, UUID userId, ChangeMemberRoleRequest request) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!project.isOwner(currentUser.id())) {
            throw new AccessDeniedException("Only owner can change roles");
        }

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Membership not found"));

        membership.setRole(request.role());
        membershipRepository.save(membership);

        UserResponse user = userClient.getUser(userId).orElse(null);

        return mapToResponse(membership, user);
    }

    @Override
    public void removeMember(UUID projectId, UUID userId) {
        Project project = getProjectOrThrow(projectId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!project.canBeUpdatedBy(currentUser.id())) {
            throw new AccessDeniedException("Not allowed to remove members");
        }

        // TODO(task-service): kada task-service bude gotov, pozvati ga i proveriti
        //  da li clan ima dodeljene zadatke na projektu; ako ima, baciti
        //  MemberHasAssignedTasksException (u monolitu: taskRepository.existsByProjectIdAndAssigneeId)

        if (project.isOwner(userId)) {
            throw new IllegalArgumentException("Cannot remove project owner");
        }

        membershipRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    private Project getProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectMemberResponse mapToResponse(ProjectMembership membership, UserResponse user) {
        return new ProjectMemberResponse(
                membership.getUserId(),
                user != null ? user.email() : null,
                user != null ? user.firstName() : null,
                user != null ? user.lastName() : null,
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
