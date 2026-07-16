package com.bozidar.tms.task_service.task;

import com.bozidar.tms.task_service.task.dto.ChangeAssigneeRequest;
import com.bozidar.tms.task_service.task.dto.ChangeStatusRequest;
import com.bozidar.tms.task_service.task.dto.TaskCreateRequest;
import com.bozidar.tms.task_service.task.dto.TaskResponse;
import com.bozidar.tms.task_service.task.dto.TaskSearchCriteria;
import com.bozidar.tms.task_service.task.dto.TaskUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    TaskResponse create(TaskCreateRequest request);

    TaskResponse getById(UUID taskId);

    List<TaskResponse> getByProject(UUID projectId);

    List<TaskResponse> search(UUID projectId, TaskSearchCriteria criteria);

    TaskResponse update(UUID taskId, TaskUpdateRequest request);

    TaskResponse changeStatus(UUID taskId, ChangeStatusRequest request);

    TaskResponse changeAssignee(UUID taskId, ChangeAssigneeRequest request);

    void delete(UUID taskId);
}
