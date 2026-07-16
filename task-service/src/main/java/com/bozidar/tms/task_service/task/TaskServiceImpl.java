package com.bozidar.tms.task_service.task;

import com.bozidar.tms.task_service.client.ProjectClient;
import com.bozidar.tms.task_service.client.UserClient;
import com.bozidar.tms.task_service.client.dto.UserResponse;
import com.bozidar.tms.task_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.task_service.security.CurrentUser;
import com.bozidar.tms.task_service.security.CurrentUserProvider;
import com.bozidar.tms.task_service.task.dto.ChangeAssigneeRequest;
import com.bozidar.tms.task_service.task.dto.ChangeStatusRequest;
import com.bozidar.tms.task_service.task.dto.TaskCreateRequest;
import com.bozidar.tms.task_service.task.dto.TaskResponse;
import com.bozidar.tms.task_service.task.dto.TaskSearchCriteria;
import com.bozidar.tms.task_service.task.dto.TaskUpdateRequest;
import com.bozidar.tms.task_service.task.search.TaskSpecifications;
import com.bozidar.tms.task_service.task.status.TaskStatus;
import com.bozidar.tms.task_service.task.status.TaskStatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final String PROJECT_MANAGER_ROLE = "PROJECT_MANAGER";

    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectClient projectClient;
    private final UserClient userClient;

    @Override
    public TaskResponse create(TaskCreateRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        projectClient.getProject(request.projectId())
                     .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        TaskStatus defaultStatus = taskStatusRepository.findByCode("TODO")
                                                       .orElseThrow(() -> new IllegalStateException(
                                                               "Default status TODO missing"));

        Task task = new Task();
        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setStatus(defaultStatus);
        task.setCreatedById(currentUser.id());

        if (request.assigneeId() != null) {
            UserResponse assignee = userClient.getUser(request.assigneeId())
                                              .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssigneeId(assignee.id());
        }

        task = taskRepository.save(task);

        // TODO(events): objaviti TaskCreated dogadjaj (audit-service, notification-service)

        return mapToResponse(task);
    }

    @Override
    public TaskResponse getById(UUID taskId) {
        return mapToResponse(getTaskOrThrow(taskId));
    }

    @Override
    public List<TaskResponse> getByProject(UUID projectId) {
        return mapAllToResponse(taskRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId));
    }

    @Override
    public List<TaskResponse> search(UUID projectId, TaskSearchCriteria criteria) {
        if (noFilters(criteria)) {
            return getByProject(projectId);
        }

        Specification<Task> spec = Specification.where(
                TaskSpecifications.projectEquals(projectId)
        );

        if (hasText(criteria.search())) {
            spec = spec.and(TaskSpecifications.titleContains(criteria.search()));
        }

        if (criteria.priority() != null) {
            spec = spec.and(TaskSpecifications.priorityEquals(criteria.priority()));
        }

        if (criteria.assigneeId() != null) {
            spec = spec.and(TaskSpecifications.assigneeEquals(criteria.assigneeId()));
        }

        if (hasText(criteria.statusCode())) {
            spec = spec.and(TaskSpecifications.statusCodeEquals(criteria.statusCode()));
        }

        if (criteria.dueDateFrom() != null) {
            spec = spec.and(TaskSpecifications.dueDateFrom(criteria.dueDateFrom()));
        }

        if (criteria.dueDateTo() != null) {
            spec = spec.and(TaskSpecifications.dueDateTo(criteria.dueDateTo()));
        }

        return mapAllToResponse(taskRepository.findAll(spec));
    }

    @Override
    public TaskResponse update(UUID taskId, TaskUpdateRequest request) {
        Task task = getTaskOrThrow(taskId);

        // TODO(events): za svaku izmenu objaviti odgovarajuci dogadjaj
        //  (TITLE_CHANGED, DESCRIPTION_CHANGED, DUE_DATE_CHANGED, PRIORITY_CHANGED)

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());

        return mapToResponse(task);
    }

    @Override
    public TaskResponse changeStatus(UUID taskId, ChangeStatusRequest request) {
        Task task = getTaskOrThrow(taskId);

        TaskStatus newStatus = taskStatusRepository.findByCode(request.statusCode())
                                                   .orElseThrow(
                                                           () -> new ResourceNotFoundException("Status not found"));

        String oldCode = task.getStatus().getCode();
        String newCode = newStatus.getCode();

        if (!oldCode.equals(newCode)) {
            // TODO(events): objaviti STATUS_CHANGED dogadjaj
            task.setStatus(newStatus);
        }

        return mapToResponse(task);
    }

    @Override
    public TaskResponse changeAssignee(UUID taskId, ChangeAssigneeRequest request) {
        Task task = getTaskOrThrow(taskId);

        UUID oldAssigneeId = task.getAssigneeId();
        UUID newAssigneeId = request.assigneeId();

        if (Objects.equals(oldAssigneeId, newAssigneeId)) {
            return mapToResponse(task);
        }

        if (newAssigneeId != null) {
            userClient.getUser(newAssigneeId)
                      .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        // TODO(events): objaviti ASSIGNEE_CHANGED / TaskAssigned dogadjaj

        task.setAssigneeId(newAssigneeId);

        return mapToResponse(task);
    }

    @Override
    public void delete(UUID taskId) {
        Task task = getTaskOrThrow(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        requireDeletePermission(task, currentUser);

        // TODO(events): objaviti TaskDeleted dogadjaj

        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                             .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void requireDeletePermission(Task task, CurrentUser user) {
        if (user.hasRole("ADMIN")) {
            return;
        }

        if (task.getCreatedById().equals(user.id())) {
            return;
        }

        boolean isProjectManager =
                projectClient.hasRole(task.getProjectId(), user.id(), PROJECT_MANAGER_ROLE);

        if (isProjectManager) {
            return;
        }

        throw new AccessDeniedException("You cannot delete this task");

    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private boolean noFilters(TaskSearchCriteria criteria) {
        return criteria == null
               || (
                       !hasText(criteria.search())
                       && criteria.priority() == null
                       && criteria.assigneeId() == null
                       && !hasText(criteria.statusCode())
                       && criteria.dueDateFrom() == null
                       && criteria.dueDateTo() == null
               );
    }

    private List<TaskResponse> mapAllToResponse(List<Task> tasks) {
        Set<UUID> userIds = new HashSet<>();
        for (Task task : tasks) {
            userIds.add(task.getCreatedById());
            if (task.getAssigneeId() != null) {
                userIds.add(task.getAssigneeId());
            }
        }

        Map<UUID, UserResponse> users = userClient.getUsersMappedByIds(userIds);

        return tasks.stream()
                    .map(task -> mapToResponse(task, users))
                    .toList();
    }

    private TaskResponse mapToResponse(Task task) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(task.getCreatedById());
        if (task.getAssigneeId() != null) {
            userIds.add(task.getAssigneeId());
        }

        return mapToResponse(task, userClient.getUsersMappedByIds(userIds));
    }

    private TaskResponse mapToResponse(Task task, Map<UUID, UserResponse> users) {
        UserResponse createdBy = users.get(task.getCreatedById());
        UserResponse assignee = task.getAssigneeId() != null ? users.get(task.getAssigneeId()) : null;

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDueDate(),
                task.getStatus().getCode(),
                task.getStatus().getName(),
                task.getProjectId(),
                task.getCreatedById(),
                createdBy != null ? createdBy.fullName() : null,
                task.getAssigneeId(),
                assignee != null ? assignee.fullName() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
