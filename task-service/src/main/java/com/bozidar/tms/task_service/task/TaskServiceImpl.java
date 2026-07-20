package com.bozidar.tms.task_service.task;

import com.bozidar.tms.task_service.client.ProjectClient;
import com.bozidar.tms.task_service.client.UserClient;
import com.bozidar.tms.task_service.client.dto.UserResponse;
import com.bozidar.tms.task_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.task_service.event.TaskEvent;
import com.bozidar.tms.task_service.event.TaskEventPublisher;
import com.bozidar.tms.task_service.event.TaskEventType;
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
    private final TaskEventPublisher eventPublisher;

    @Override
    public TaskResponse create(TaskCreateRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!projectClient.isMember(request.projectId(), currentUser.id())) {
            throw new AccessDeniedException("Only project members can create tasks");
        }

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
            if (!projectClient.isMember(request.projectId(), request.assigneeId())) {
                throw new IllegalArgumentException("Assignee must be a project member");
            }
            task.setAssigneeId(request.assigneeId());
        }

        task = taskRepository.save(task);

        eventPublisher.publish(taskEvent(TaskEventType.TASK_CREATED, task, currentUser, null, task.getTitle()));

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
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!request.title().equals(task.getTitle())) {
            eventPublisher.publish(taskEvent(TaskEventType.TITLE_CHANGED, task, currentUser,
                                             task.getTitle(), request.title()));
            task.setTitle(request.title());
        }

        if (!Objects.equals(request.description(), task.getDescription())) {
            eventPublisher.publish(taskEvent(TaskEventType.DESCRIPTION_CHANGED, task, currentUser,
                                             task.getDescription(), request.description()));
            task.setDescription(request.description());
        }

        if (!Objects.equals(request.dueDate(), task.getDueDate())) {
            eventPublisher.publish(taskEvent(TaskEventType.DUE_DATE_CHANGED, task, currentUser,
                                             String.valueOf(task.getDueDate()), String.valueOf(request.dueDate())));
            task.setDueDate(request.dueDate());
        }

        if (request.priority() != task.getPriority()) {
            eventPublisher.publish(taskEvent(TaskEventType.PRIORITY_CHANGED, task, currentUser,
                                             String.valueOf(task.getPriority()), String.valueOf(request.priority())));
            task.setPriority(request.priority());
        }

        return mapToResponse(task);
    }

    @Override
    public TaskResponse changeStatus(UUID taskId, ChangeStatusRequest request) {
        Task task = getTaskOrThrow(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        TaskStatus newStatus = taskStatusRepository.findByCode(request.statusCode())
                                                   .orElseThrow(
                                                           () -> new ResourceNotFoundException("Status not found"));

        String oldCode = task.getStatus().getCode();
        String newCode = newStatus.getCode();

        if (!oldCode.equals(newCode)) {
            eventPublisher.publish(taskEvent(TaskEventType.STATUS_CHANGED, task, currentUser, oldCode, newCode));
            task.setStatus(newStatus);
        }

        return mapToResponse(task);
    }

    @Override
    public TaskResponse changeAssignee(UUID taskId, ChangeAssigneeRequest request) {
        Task task = getTaskOrThrow(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        UUID oldAssigneeId = task.getAssigneeId();
        UUID newAssigneeId = request.assigneeId();

        if (Objects.equals(oldAssigneeId, newAssigneeId)) {
            return mapToResponse(task);
        }

        if (newAssigneeId != null && !projectClient.isMember(task.getProjectId(), newAssigneeId)) {
            throw new IllegalArgumentException("Assignee must be a project member");
        }

        Set<UUID> assigneeIds = new HashSet<>();
        if (oldAssigneeId != null) {
            assigneeIds.add(oldAssigneeId);
        }
        if (newAssigneeId != null) {
            assigneeIds.add(newAssigneeId);
        }

        Map<UUID, UserResponse> assignees = userClient.getUsersMappedByIds(assigneeIds);

        String from = oldAssigneeId == null ? "Unassigned"
                : assignees.containsKey(oldAssigneeId) ? assignees.get(oldAssigneeId).fullName() : "Unknown";
        String to = newAssigneeId == null ? "Unassigned" : assignees.get(newAssigneeId).fullName();

        task.setAssigneeId(newAssigneeId);

        eventPublisher.publish(taskEvent(TaskEventType.ASSIGNEE_CHANGED, task, currentUser, from, to));

        return mapToResponse(task);
    }

    @Override
    public void delete(UUID taskId) {
        Task task = getTaskOrThrow(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        requireDeletePermission(task, currentUser);

        eventPublisher.publish(taskEvent(TaskEventType.TASK_DELETED, task, currentUser, task.getTitle(), null));

        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                             .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private TaskEvent taskEvent(TaskEventType type, Task task, CurrentUser actor,
                                String oldValue, String newValue) {
        return TaskEvent.of(
                type,
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                oldValue,
                newValue,
                actor.id(),
                actor.fullName(),
                task.getAssigneeId()
        );
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
