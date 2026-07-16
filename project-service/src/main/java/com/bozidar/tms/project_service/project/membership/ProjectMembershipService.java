package com.bozidar.tms.project_service.project.membership;

import com.bozidar.tms.project_service.project.membership.dto.AddMemberRequest;
import com.bozidar.tms.project_service.project.membership.dto.ChangeMemberRoleRequest;
import com.bozidar.tms.project_service.project.membership.dto.ProjectMemberResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectMembershipService {

    ProjectMemberResponse addMember(UUID projectId, AddMemberRequest request);

    List<ProjectMemberResponse> listMembers(UUID projectId);

    ProjectMemberResponse getMember(UUID projectId, UUID userId);

    ProjectMemberResponse changeRole(UUID projectId, UUID userId, ChangeMemberRoleRequest request);

    void removeMember(UUID projectId, UUID userId);
}
